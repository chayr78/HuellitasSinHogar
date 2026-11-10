package com.Huellitas.Huellitas.service;

import com.Huellitas.Huellitas.config.UsuarioAutenticado;
import com.Huellitas.Huellitas.dto.CompatibilidadRequest;
import com.Huellitas.Huellitas.dto.CompatibilidadResponse;
import com.Huellitas.Huellitas.dto.CrearSolicitudRequest;
import com.Huellitas.Huellitas.dto.SolicitudResponse;
import com.Huellitas.Huellitas.exception.ApiException;
import com.Huellitas.Huellitas.modelo.Constantes;
import com.Huellitas.Huellitas.modelo.Mascota;
import com.Huellitas.Huellitas.modelo.SolicitudAdopcion;
import com.Huellitas.Huellitas.modelo.Usuario;
import com.Huellitas.Huellitas.repository.MascotaRepository;
import com.Huellitas.Huellitas.repository.SolicitudRepository;
import com.Huellitas.Huellitas.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 * Solicitudes de adopción: creación, cambios de estado, marcas de lectura
 * y cálculo del porcentaje de compatibilidad.
 */
@Service
public class SolicitudService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudService.class);

    // Nombre del índice único de sql/indice_solicitud_activa.sql
    private static final String INDICE_SOLICITUD_ACTIVA = "ux_solicitud_activa";
    // Debajo de este porcentaje se muestra una advertencia al adoptante
    private static final int PORCENTAJE_MINIMO_RECOMENDADO = 75;

    private static final String MENSAJE_YA_ENVIADA = "Ya enviaste una solicitud para esta mascota y sigue en revisión";
    private static final String MENSAJE_OTRA_ACTIVA = "El adoptante ya tiene otra solicitud activa para esta mascota";

    private final SolicitudRepository solicitudRepository;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MascotaService mascotaService;
    private final EmailService emailService;

    public SolicitudService(SolicitudRepository solicitudRepository,
                            MascotaRepository mascotaRepository,
                            UsuarioRepository usuarioRepository,
                            MascotaService mascotaService,
                            EmailService emailService) {
        this.solicitudRepository = solicitudRepository;
        this.mascotaRepository = mascotaRepository;
        this.usuarioRepository = usuarioRepository;
        this.mascotaService = mascotaService;
        this.emailService = emailService;
    }

    // ---------- Consultas ----------

    // Historial del adoptante autenticado
    public List<SolicitudResponse> obtenerMias(UsuarioAutenticado usuario) {
        return toResponse(solicitudRepository.findByAdoptanteIdOrderByFechaSolicitudDesc(usuario.id()));
    }

    // RF-13: advertir si el adoptante ya tiene una solicitud activa (PENDIENTE o CONTACTADA)
    public boolean tieneSolicitudActiva(UsuarioAutenticado usuario) {
        return solicitudRepository.existsByAdoptanteIdAndEstadoIn(usuario.id(), Constantes.SOLICITUDES_ACTIVAS);
    }

    // RF-06: solicitudes recibidas para una mascota. Solo su publicador o un administrador.
    public List<SolicitudResponse> obtenerPorMascota(Long mascotaId, UsuarioAutenticado usuario) {
        Mascota mascota = mascotaService.buscar(mascotaId);
        mascotaService.verificarDuenoOAdmin(mascota, usuario,
                "Solo quien publicó esta mascota puede ver sus solicitudes");
        return toResponse(solicitudRepository.findByMascotaIdOrderByFechaSolicitudDesc(mascotaId));
    }

    // ---------- Creación ----------

    /*
     * RF-10 + RF-12: crea la solicitud. Si vienen respuestas de la encuesta
     * (tipoVivienda y horasDisponibles), calcula el porcentaje de compatibilidad.
     */
    public SolicitudResponse crear(CrearSolicitudRequest req, UsuarioAutenticado adoptante) {
        boolean traeVivienda = req.tipoVivienda() != null;
        boolean traeHoras = req.horasDisponibles() != null;
        if (traeVivienda != traeHoras) {
            throw ApiException.badRequest("Para la encuesta indica tanto el tipo de vivienda como las horas disponibles");
        }

        Mascota mascota = mascotaRepository.findById(req.mascotaId()).orElse(null);
        if (mascota == null) {
            throw ApiException.badRequest("La mascota solicitada no existe");
        }
        if (!Constantes.MASCOTA_DISPONIBLE.equals(mascota.getEstado())) {
            throw ApiException.badRequest("Esta mascota ya no está disponible para adopción");
        }
        if (solicitudRepository.existsByMascotaIdAndAdoptanteIdAndEstadoIn(
                mascota.getId(), adoptante.id(), Constantes.SOLICITUDES_ACTIVAS)) {
            throw ApiException.conflicto(MENSAJE_YA_ENVIADA);
        }

        SolicitudAdopcion solicitud = nuevaSolicitud(req, mascota, adoptante);
        if (traeVivienda && traeHoras) {
            short porcentaje = calcularCompatibilidad(req.tipoVivienda(), req.horasDisponibles(),
                    mascota.getTamano(), mascota.getNivelEnergia());
            solicitud.setPorcentajeCompatibilidad(porcentaje);
        }

        SolicitudAdopcion guardada = guardarSolicitudActiva(solicitud,
                MENSAJE_YA_ENVIADA);
        notificarPublicador(mascota, guardada);
        boolean correoEnviado = confirmarAlAdoptante(mascota, guardada);

        // El adoptante que acaba de enviar la solicitud recibe el WhatsApp del publicador (null si no tiene)
        Usuario publicador = usuarioRepository.findById(mascota.getPublicadorId()).orElse(null);
        Usuario usuarioAdoptante = usuarioRepository.findById(adoptante.id()).orElse(null);
        String whatsapp = null;
        if (publicador != null) {
            whatsapp = Textos.soloDigitos(publicador.getTelefono());
        }
        return toResponse(guardada, mascota, publicador, usuarioAdoptante, whatsapp, correoEnviado);
    }

    // Copia los datos del formulario a una solicitud nueva en estado PENDIENTE
    private static SolicitudAdopcion nuevaSolicitud(CrearSolicitudRequest req, Mascota mascota,
                                                    UsuarioAutenticado adoptante) {
        SolicitudAdopcion solicitud = new SolicitudAdopcion();
        solicitud.setMascotaId(mascota.getId());
        solicitud.setAdoptanteId(adoptante.id());
        solicitud.setNombreContacto(req.nombreContacto().trim());
        solicitud.setTelefonoContacto(req.telefonoContacto().trim());
        solicitud.setCorreoContacto(req.correoContacto().trim());
        solicitud.setMensaje(Textos.textoOpcional(req.mensaje()));
        solicitud.setEstado(Constantes.SOLICITUD_PENDIENTE);
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setTipoVivienda(req.tipoVivienda());
        solicitud.setHorasDisponibles(req.horasDisponibles());
        solicitud.setExperienciaPrevia(req.experienciaPrevia());
        solicitud.setNinosPequenos(req.ninosPequenos());
        solicitud.setOtrasMascotas(req.otrasMascotas());
        // El publicador tiene una notificación nueva; el adoptante ya conoce su propia solicitud
        solicitud.setLeidaPublicador(false);
        solicitud.setLeidaAdoptante(true);
        return solicitud;
    }

    // Avisa por correo al publicador. Un fallo aquí nunca debe romper la creación de la solicitud.
    private void notificarPublicador(Mascota mascota, SolicitudAdopcion solicitud) {
        try {
            Usuario publicador = usuarioRepository.findById(mascota.getPublicadorId()).orElse(null);
            if (publicador == null) {
                return;
            }
            String asunto = "Nueva solicitud para " + mascota.getNombre();
            String cuerpo = "Hola " + publicador.getNombre() + ",\n\n"
                    + solicitud.getNombreContacto() + " envió una solicitud para adoptar a "
                    + mascota.getNombre() + ".\n"
                    + "Revísala en tu panel de Huellitas sin Hogar.";
            boolean enviado = emailService.enviar(publicador.getCorreo(), asunto, cuerpo);
            if (!enviado) {
                log.info("[DEV] Aviso al publicador {}: {} envió una solicitud para {}",
                        publicador.getId(), solicitud.getNombreContacto(), mascota.getNombre());
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo notificar al publicador de la solicitud {}: {}", solicitud.getId(), e.getMessage());
        }
    }

    // Confirma por correo al adoptante (al correo que escribió en la solicitud). Devuelve true si se envió;
    // igual que el aviso al publicador, un fallo aquí nunca rompe la creación de la solicitud.
    private boolean confirmarAlAdoptante(Mascota mascota, SolicitudAdopcion solicitud) {
        try {
            String asunto = "Recibimos tu solicitud para adoptar a " + mascota.getNombre();
            String cuerpo = "Hola " + solicitud.getNombreContacto() + ",\n\n"
                    + "Tu solicitud para adoptar a " + mascota.getNombre() + " se envió correctamente.\n"
                    + "Quien la publicó la revisará y se pondrá en contacto contigo.\n"
                    + "Puedes ver cómo va en \"Mis solicitudes\" de Huellitas sin Hogar.";
            boolean enviado = emailService.enviar(solicitud.getCorreoContacto(), asunto, cuerpo);
            if (!enviado) {
                log.info("[DEV] Confirmación al adoptante {}: solicitud para {}",
                        solicitud.getAdoptanteId(), mascota.getNombre());
            }
            return enviado;
        } catch (RuntimeException e) {
            log.warn("No se pudo confirmar al adoptante la solicitud {}: {}", solicitud.getId(), e.getMessage());
            return false;
        }
    }

    // ---------- Compatibilidad ----------

    // Calcula la compatibilidad contra una mascota sin crear la solicitud
    public CompatibilidadResponse calcularCompatibilidad(CompatibilidadRequest req) {
        Mascota mascota = mascotaService.buscar(req.mascotaId());
        short porcentaje = calcularCompatibilidad(req.tipoVivienda(), req.horasDisponibles(),
                mascota.getTamano(), mascota.getNivelEnergia());
        boolean advertencia = porcentaje < PORCENTAJE_MINIMO_RECOMENDADO;
        return new CompatibilidadResponse(porcentaje, advertencia);
    }

    /*
     * Fórmula de compatibilidad definida en la especificación funcional:
     * - Puntos por vivienda/tamaño (0, 1 o 2) + puntos por horas/energía (0, 1 o 2)
     * - Porcentaje = (puntos obtenidos / 4) * 100  -> 0, 25, 50, 75 o 100
     */
    public short calcularCompatibilidad(String tipoVivienda, String horasDisponibles,
                                        String tamano, String nivelEnergia) {
        int puntosVivienda = puntosViviendaTamano(tipoVivienda, tamano);
        int puntosHoras = puntosHorasEnergia(horasDisponibles, nivelEnergia);
        int totalPuntos = puntosVivienda + puntosHoras; // máximo 4
        return (short) ((totalPuntos / 4.0) * 100);
    }

    // Cuanto más pequeña la vivienda, peor encaja con una mascota grande
    private static int puntosViviendaTamano(String tipoVivienda, String tamano) {
        if ("DEPTO_SIN_PATIO".equals(tipoVivienda)) {
            if ("CHICO".equals(tamano)) {
                return 2;
            }
            if ("MEDIANO".equals(tamano)) {
                return 1;
            }
            return 0; // GRANDE
        }
        if ("PATIO_PEQUENO".equals(tipoVivienda)) {
            if ("CHICO".equals(tamano) || "MEDIANO".equals(tamano)) {
                return 2;
            }
            return 1; // GRANDE
        }
        return 2; // PATIO_GRANDE: compatible con cualquier tamaño
    }

    // Cuanto menos tiempo disponible, peor encaja con una mascota de mucha energía
    private static int puntosHorasEnergia(String horasDisponibles, String nivelEnergia) {
        if ("MENOS_2H".equals(horasDisponibles)) {
            if ("BAJA".equals(nivelEnergia)) {
                return 2;
            }
            if ("MEDIA".equals(nivelEnergia)) {
                return 1;
            }
            return 0; // ALTA
        }
        if ("DE_2_A_5H".equals(horasDisponibles)) {
            if ("BAJA".equals(nivelEnergia) || "MEDIA".equals(nivelEnergia)) {
                return 2;
            }
            return 1; // ALTA
        }
        return 2; // MAS_5H: compatible con cualquier nivel de energía
    }

    // ---------- Cambios de estado y lectura ----------

    // RF-06: el publicador dueño de la mascota actualiza el estado de una solicitud
    public SolicitudResponse cambiarEstado(Long id, String estado, UsuarioAutenticado usuario) {
        String nuevoEstado = estado.trim().toUpperCase(Locale.ROOT);
        if (!esEstadoValido(nuevoEstado)) {
            throw ApiException.badRequest("El estado debe ser PENDIENTE, CONTACTADA o CERRADA");
        }

        SolicitudAdopcion solicitud = buscar(id);
        Mascota mascota = verificarPublicadorDueno(solicitud, usuario);
        if (nuevoEstado.equals(solicitud.getEstado())) {
            return toResponse(solicitud);
        }

        boolean estabaCerrada = Constantes.SOLICITUD_CERRADA.equals(solicitud.getEstado());
        boolean pasaAActiva = Constantes.SOLICITUDES_ACTIVAS.contains(nuevoEstado);
        if (estabaCerrada && pasaAActiva) {
            validarReapertura(solicitud, mascota);
        }

        solicitud.setEstado(nuevoEstado);
        solicitud.setLeidaAdoptante(false); // el adoptante aún no ha visto este cambio
        return toResponse(guardarSolicitudActiva(solicitud,
                MENSAJE_OTRA_ACTIVA));
    }

    // Reabrir una solicitud cerrada no debe dejar dos activas ni reactivar una mascota ya adoptada
    private void validarReapertura(SolicitudAdopcion solicitud, Mascota mascota) {
        if (Constantes.MASCOTA_ADOPTADO.equals(mascota.getEstado())) {
            throw ApiException.badRequest("No se puede reabrir una solicitud de una mascota ya adoptada");
        }
        if (solicitudRepository.existsByMascotaIdAndAdoptanteIdAndEstadoIn(
                solicitud.getMascotaId(), solicitud.getAdoptanteId(), Constantes.SOLICITUDES_ACTIVAS)) {
            throw ApiException.conflicto(MENSAJE_OTRA_ACTIVA);
        }
    }

    public SolicitudResponse marcarLeidaPorPublicador(Long id, UsuarioAutenticado usuario) {
        SolicitudAdopcion solicitud = buscar(id);
        verificarPublicadorDueno(solicitud, usuario);
        solicitud.setLeidaPublicador(true);
        return toResponse(solicitudRepository.save(solicitud));
    }

    public SolicitudResponse marcarLeidaPorAdoptante(Long id, UsuarioAutenticado usuario) {
        SolicitudAdopcion solicitud = buscar(id);
        if (!solicitud.getAdoptanteId().equals(usuario.id())) {
            throw ApiException.prohibido("Esta solicitud no es tuya");
        }
        solicitud.setLeidaAdoptante(true);
        return toResponse(solicitudRepository.save(solicitud));
    }

    // ---------- Utilidades ----------

    /*
     * Las comprobaciones previas no bastan con peticiones simultáneas (doble clic):
     * el índice único parcial ux_solicitud_activa (sql/indice_solicitud_activa.sql)
     * rechaza la segunda y aquí se convierte en un 409 con un mensaje claro.
     */
    private SolicitudAdopcion guardarSolicitudActiva(SolicitudAdopcion solicitud, String mensajeDuplicada) {
        try {
            return solicitudRepository.saveAndFlush(solicitud);
        } catch (DataIntegrityViolationException e) {
            String causa = e.getMostSpecificCause().getMessage();
            if (causa != null && causa.contains(INDICE_SOLICITUD_ACTIVA)) {
                throw ApiException.conflicto(mensajeDuplicada);
            }
            throw e;
        }
    }

    private SolicitudAdopcion buscar(Long id) {
        SolicitudAdopcion solicitud = solicitudRepository.findById(id).orElse(null);
        if (solicitud == null) {
            throw ApiException.noEncontrado("No existe una solicitud con id: " + id);
        }
        return solicitud;
    }

    // Solo el publicador de la mascota (el administrador no) gestiona sus solicitudes
    private Mascota verificarPublicadorDueno(SolicitudAdopcion solicitud, UsuarioAutenticado usuario) {
        Mascota mascota = mascotaService.buscar(solicitud.getMascotaId());
        if (!mascota.getPublicadorId().equals(usuario.id())) {
            throw ApiException.prohibido("Solo quien publicó la mascota puede gestionar sus solicitudes");
        }
        return mascota;
    }

    private static boolean esEstadoValido(String estado) {
        return Constantes.SOLICITUD_PENDIENTE.equals(estado)
                || Constantes.SOLICITUD_CONTACTADA.equals(estado)
                || Constantes.SOLICITUD_CERRADA.equals(estado);
    }

    // ---------- Conversión a DTO de respuesta ----------

    public SolicitudResponse toResponse(SolicitudAdopcion solicitud) {
        Mascota mascota = mascotaRepository.findById(solicitud.getMascotaId()).orElse(null);
        Usuario publicador = null;
        if (mascota != null) {
            publicador = usuarioRepository.findById(mascota.getPublicadorId()).orElse(null);
        }
        Usuario adoptante = usuarioRepository.findById(solicitud.getAdoptanteId()).orElse(null);
        return toResponse(solicitud, mascota, publicador, adoptante, null, null);
    }

    // Versión para listas: busca mascotas, publicadores y adoptantes de toda la lista en tres consultas
    public List<SolicitudResponse> toResponse(List<SolicitudAdopcion> solicitudes) {
        List<Long> mascotaIds = new ArrayList<>();
        List<Long> adoptanteIds = new ArrayList<>();
        for (SolicitudAdopcion solicitud : solicitudes) {
            if (!mascotaIds.contains(solicitud.getMascotaId())) {
                mascotaIds.add(solicitud.getMascotaId());
            }
            if (!adoptanteIds.contains(solicitud.getAdoptanteId())) {
                adoptanteIds.add(solicitud.getAdoptanteId());
            }
        }

        // id de la mascota -> mascota, y de paso los ids de sus publicadores
        Map<Long, Mascota> mascotas = new HashMap<>();
        List<Long> publicadorIds = new ArrayList<>();
        for (Mascota mascota : mascotaRepository.findAllById(mascotaIds)) {
            mascotas.put(mascota.getId(), mascota);
            if (!publicadorIds.contains(mascota.getPublicadorId())) {
                publicadorIds.add(mascota.getPublicadorId());
            }
        }

        // id del publicador -> publicador
        Map<Long, Usuario> publicadores = new HashMap<>();
        for (Usuario usuario : usuarioRepository.findAllById(publicadorIds)) {
            publicadores.put(usuario.getId(), usuario);
        }

        // id del adoptante -> adoptante
        Map<Long, Usuario> adoptantes = new HashMap<>();
        for (Usuario usuario : usuarioRepository.findAllById(adoptanteIds)) {
            adoptantes.put(usuario.getId(), usuario);
        }

        List<SolicitudResponse> respuesta = new ArrayList<>();
        for (SolicitudAdopcion solicitud : solicitudes) {
            Mascota mascota = mascotas.get(solicitud.getMascotaId());
            Usuario publicador = null;
            if (mascota != null) {
                publicador = publicadores.get(mascota.getPublicadorId());
            }
            Usuario adoptante = adoptantes.get(solicitud.getAdoptanteId());
            respuesta.add(toResponse(solicitud, mascota, publicador, adoptante, null, null));
        }
        return respuesta;
    }

    // mascota, publicador y adoptante pueden ser null si ya no existen.
    // whatsappPublicador y correoConfirmacionEnviado solo se pasan al responder al adoptante que creó la solicitud.
    private static SolicitudResponse toResponse(SolicitudAdopcion s, Mascota mascota,
                                                Usuario publicador, Usuario adoptante,
                                                String whatsappPublicador, Boolean correoConfirmacionEnviado) {
        String mascotaNombre = null;
        String mascotaFotoUrl = null;
        if (mascota != null) {
            mascotaNombre = mascota.getNombre();
            mascotaFotoUrl = mascota.getFotoUrl();
        }
        String publicadorNombre = null;
        if (publicador != null) {
            publicadorNombre = publicador.getNombre();
        }
        String adoptanteNombre = null;
        String adoptanteFotoUrl = null;
        if (adoptante != null) {
            adoptanteNombre = adoptante.getNombre();
            adoptanteFotoUrl = adoptante.getFotoPerfilUrl();
        }

        return new SolicitudResponse(
                s.getId(),
                s.getMascotaId(),
                mascotaNombre,
                mascotaFotoUrl,
                publicadorNombre,
                s.getAdoptanteId(),
                s.getNombreContacto(),
                s.getTelefonoContacto(),
                s.getCorreoContacto(),
                s.getMensaje(),
                s.getEstado(),
                s.getFechaSolicitud(),
                s.getTipoVivienda(),
                s.getHorasDisponibles(),
                s.getExperienciaPrevia(),
                s.getNinosPequenos(),
                s.getOtrasMascotas(),
                s.getPorcentajeCompatibilidad(),
                s.getLeidaPublicador(),
                s.getLeidaAdoptante(),
                adoptanteNombre,
                adoptanteFotoUrl,
                whatsappPublicador,
                correoConfirmacionEnviado
        );
    }
}

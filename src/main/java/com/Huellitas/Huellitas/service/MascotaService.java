package com.Huellitas.Huellitas.service;

import com.Huellitas.Huellitas.config.UsuarioAutenticado;
import com.Huellitas.Huellitas.dto.MascotaRequest;
import com.Huellitas.Huellitas.dto.MascotaResponse;
import com.Huellitas.Huellitas.exception.ApiException;
import com.Huellitas.Huellitas.modelo.Constantes;
import com.Huellitas.Huellitas.modelo.Mascota;
import com.Huellitas.Huellitas.modelo.MascotaFoto;
import com.Huellitas.Huellitas.modelo.SolicitudAdopcion;
import com.Huellitas.Huellitas.modelo.Usuario;
import com.Huellitas.Huellitas.repository.MascotaFotoRepository;
import com.Huellitas.Huellitas.repository.MascotaRepository;
import com.Huellitas.Huellitas.repository.SolicitudRepository;
import com.Huellitas.Huellitas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 * Publicación, edición, cambio de estado y consulta de mascotas.
 */
@Service
public class MascotaService {

    private static final String MENSAJE_SOLO_DUENO = "Solo quien publicó esta mascota puede modificarla";
    private static final int MAXIMO_FOTOS_ADICIONALES = 5;
    private static final String MENSAJE_SIN_PORTADA = "Primero elige la foto principal.";

    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudRepository solicitudRepository;
    private final MascotaFotoRepository mascotaFotoRepository;
    // Prefijo que deben tener las fotos adicionales (Supabase Storage público)
    private final String prefijoFotos;

    public MascotaService(MascotaRepository mascotaRepository,
                          UsuarioRepository usuarioRepository,
                          SolicitudRepository solicitudRepository,
                          MascotaFotoRepository mascotaFotoRepository,
                          @Value("${app.supabase.url}") String supabaseUrl) {
        this.mascotaRepository = mascotaRepository;
        this.usuarioRepository = usuarioRepository;
        this.solicitudRepository = solicitudRepository;
        this.mascotaFotoRepository = mascotaFotoRepository;
        this.prefijoFotos = supabaseUrl.replaceAll("/+$", "") + "/storage/v1/object/public/";
    }

    // ---------- Consultas ----------

    public List<MascotaResponse> obtenerTodas() {
        return toResponse(mascotaRepository.findAllByOrderByFechaPublicacionDesc());
    }

    // Catálogo público: solo disponibles
    public List<MascotaResponse> obtenerDisponibles() {
        return toResponse(mascotaRepository.findByEstadoOrderByFechaPublicacionDesc(Constantes.MASCOTA_DISPONIBLE));
    }

    // Historias felices. No se guarda la fecha de adopción, así que se ordenan por publicación.
    public List<MascotaResponse> obtenerAdoptados() {
        return toResponse(mascotaRepository.findByEstadoOrderByFechaPublicacionDesc(Constantes.MASCOTA_ADOPTADO));
    }

    // Catálogo con filtro de tamaño (solo disponibles)
    public List<MascotaResponse> filtrarPorTamano(String tamano) {
        String tamanoNormalizado = tamano.trim().toUpperCase(Locale.ROOT);
        return toResponse(mascotaRepository.findByTamanoAndEstadoOrderByFechaPublicacionDesc(
                tamanoNormalizado, Constantes.MASCOTA_DISPONIBLE));
    }

    public List<MascotaResponse> obtenerDelPublicador(Long publicadorId) {
        return toResponse(mascotaRepository.findByPublicadorIdOrderByFechaPublicacionDesc(publicadorId));
    }

    public MascotaResponse obtenerPorId(Long id) {
        return toResponse(buscar(id));
    }

    // ---------- Modificaciones ----------

    // RF-03: el publicador sale del token, nunca del body
    @Transactional
    public MascotaResponse crear(MascotaRequest req, UsuarioAutenticado usuario) {
        List<String> fotos = limpiarFotos(req.fotosAdicionales(), req.fotoUrl());

        Mascota mascota = new Mascota();
        copiarDatos(req, mascota);
        mascota.setPublicadorId(usuario.id());
        mascota.setEstado(Constantes.MASCOTA_DISPONIBLE);
        mascota.setFechaPublicacion(LocalDateTime.now());
        Mascota guardada = mascotaRepository.save(mascota);

        if (fotos != null) {
            guardarFotos(guardada.getId(), fotos);
        }
        return toResponse(guardada);
    }

    // RF-04. El estado no se cambia aquí, solo con PATCH /{id}/estado
    @Transactional
    public MascotaResponse actualizar(Long id, MascotaRequest req, UsuarioAutenticado usuario) {
        Mascota mascota = buscar(id);
        verificarDuenoOAdmin(mascota, usuario, MENSAJE_SOLO_DUENO);
        // Una mascota adoptada queda como registro histórico
        if (Constantes.MASCOTA_ADOPTADO.equals(mascota.getEstado())) {
            throw ApiException.conflicto("Esta mascota ya fue adoptada y sus datos no se pueden editar.");
        }

        // null = el body no trae el campo y las fotos adicionales se quedan como están
        List<String> fotos = limpiarFotos(req.fotosAdicionales(), req.fotoUrl());
        if (fotos == null && Textos.textoOpcional(req.fotoUrl()) == null
                && !mascotaFotoRepository.findByMascotaIdOrderByOrdenAsc(id).isEmpty()) {
            // Quitar la portada dejaría fotos adicionales sin foto principal
            throw ApiException.badRequest(MENSAJE_SIN_PORTADA);
        }

        copiarDatos(req, mascota);
        Mascota guardada = mascotaRepository.save(mascota);

        if (fotos != null) {
            mascotaFotoRepository.deleteByMascotaId(id);
            guardarFotos(id, fotos);
        }
        return toResponse(guardada);
    }

    // RF-05. Al pasar a ADOPTADO se cierran las solicitudes que seguían activas.
    @Transactional
    public MascotaResponse cambiarEstado(Long id, String estado, UsuarioAutenticado usuario) {
        String nuevoEstado = estado.trim().toUpperCase(Locale.ROOT);
        if (!esEstadoValido(nuevoEstado)) {
            throw ApiException.badRequest("El estado debe ser DISPONIBLE, EN_PROCESO o ADOPTADO");
        }

        Mascota mascota = buscar(id);
        verificarDuenoOAdmin(mascota, usuario, MENSAJE_SOLO_DUENO);
        mascota.setEstado(nuevoEstado);

        if (Constantes.MASCOTA_ADOPTADO.equals(nuevoEstado)) {
            cerrarSolicitudesActivas(id);
        }
        return toResponse(mascotaRepository.save(mascota));
    }

    // Elimina la mascota junto con sus solicitudes
    @Transactional
    public void eliminar(Long id, UsuarioAutenticado usuario) {
        Mascota mascota = buscar(id);
        verificarDuenoOAdmin(mascota, usuario, MENSAJE_SOLO_DUENO);
        solicitudRepository.deleteByMascotaId(id);
        mascotaRepository.delete(mascota);
    }

    // ---------- Utilidades (también las usa SolicitudService) ----------

    public Mascota buscar(Long id) {
        Mascota mascota = mascotaRepository.findById(id).orElse(null);
        if (mascota == null) {
            throw ApiException.noEncontrado("No existe una mascota con id: " + id);
        }
        return mascota;
    }

    // Lanza 403 con el mensaje indicado si el usuario no es el dueño ni un administrador
    public void verificarDuenoOAdmin(Mascota mascota, UsuarioAutenticado usuario, String mensajeError) {
        boolean esDueno = mascota.getPublicadorId().equals(usuario.id());
        if (!esDueno && !usuario.esAdmin()) {
            throw ApiException.prohibido(mensajeError);
        }
    }

    // ---------- Utilidades privadas ----------

    private static boolean esEstadoValido(String estado) {
        return Constantes.MASCOTA_DISPONIBLE.equals(estado)
                || Constantes.MASCOTA_EN_PROCESO.equals(estado)
                || Constantes.MASCOTA_ADOPTADO.equals(estado);
    }

    // Las solicitudes que seguían en revisión pasan a CERRADA y el adoptante recibe aviso
    private void cerrarSolicitudesActivas(Long mascotaId) {
        List<SolicitudAdopcion> activas =
                solicitudRepository.findByMascotaIdAndEstadoIn(mascotaId, Constantes.SOLICITUDES_ACTIVAS);
        for (SolicitudAdopcion solicitud : activas) {
            solicitud.setEstado(Constantes.SOLICITUD_CERRADA);
            solicitud.setLeidaAdoptante(false);
        }
        solicitudRepository.saveAll(activas);
    }

    /*
     * Prepara la lista de fotos adicionales: quita vacías y repetidas (también la portada)
     * y valida cantidad, URL y que exista foto principal. Devuelve null si la lista no vino.
     */
    private List<String> limpiarFotos(List<String> recibidas, String portada) {
        if (recibidas == null) {
            return null;
        }
        String portadaLimpia = Textos.textoOpcional(portada);

        List<String> fotos = new ArrayList<>();
        for (String recibida : recibidas) {
            String url = Textos.textoOpcional(recibida);
            if (url == null || url.equals(portadaLimpia) || fotos.contains(url)) {
                continue;
            }
            fotos.add(url);
        }

        if (fotos.size() > MAXIMO_FOTOS_ADICIONALES) {
            throw ApiException.badRequest("Puedes agregar hasta 5 fotos adicionales.");
        }
        for (String url : fotos) {
            if (!url.startsWith(prefijoFotos) || url.length() > 255) {
                throw ApiException.badRequest("Una de las fotos no es válida.");
            }
        }
        if (!fotos.isEmpty() && portadaLimpia == null) {
            throw ApiException.badRequest(MENSAJE_SIN_PORTADA);
        }
        return fotos;
    }

    // Inserta una fila por foto con orden 0, 1, 2...
    private void guardarFotos(Long mascotaId, List<String> fotos) {
        List<MascotaFoto> filas = new ArrayList<>();
        for (int i = 0; i < fotos.size(); i++) {
            MascotaFoto foto = new MascotaFoto();
            foto.setMascotaId(mascotaId);
            foto.setFotoUrl(fotos.get(i));
            foto.setOrden(i);
            filas.add(foto);
        }
        mascotaFotoRepository.saveAll(filas);
    }

    private static void copiarDatos(MascotaRequest req, Mascota mascota) {
        mascota.setNombre(req.nombre().trim());
        mascota.setEspecie(req.especie());
        mascota.setSexo(req.sexo());
        mascota.setEdadAproximada(Textos.textoOpcional(req.edadAproximada()));
        mascota.setFotoUrl(Textos.textoOpcional(req.fotoUrl()));
        mascota.setTamano(req.tamano());
        mascota.setNivelEnergia(req.nivelEnergia());
        mascota.setDescripcion(Textos.textoOpcional(req.descripcion()));
    }

    // ---------- Conversión a DTO de respuesta ----------

    // Convierte una mascota agregando los datos públicos de su publicador y sus fotos adicionales
    public MascotaResponse toResponse(Mascota mascota) {
        Usuario publicador = usuarioRepository.findById(mascota.getPublicadorId()).orElse(null);
        return toResponse(mascota, publicador, obtenerFotosAdicionales(mascota.getId()));
    }

    /*
     * Versión para listas: busca todos los publicadores en una sola consulta.
     * Las fotos adicionales van vacías para no hacer una consulta por cada mascota;
     * el detalle (GET /api/mascotas/{id}) sí las trae.
     */
    public List<MascotaResponse> toResponse(List<Mascota> mascotas) {
        List<Long> publicadorIds = new ArrayList<>();
        for (Mascota mascota : mascotas) {
            if (!publicadorIds.contains(mascota.getPublicadorId())) {
                publicadorIds.add(mascota.getPublicadorId());
            }
        }

        // id del publicador -> publicador
        Map<Long, Usuario> publicadores = new HashMap<>();
        for (Usuario usuario : usuarioRepository.findAllById(publicadorIds)) {
            publicadores.put(usuario.getId(), usuario);
        }

        List<MascotaResponse> respuesta = new ArrayList<>();
        for (Mascota mascota : mascotas) {
            respuesta.add(toResponse(mascota, publicadores.get(mascota.getPublicadorId()), new ArrayList<>()));
        }
        return respuesta;
    }

    // URLs de las fotos adicionales en el orden de la galería
    private List<String> obtenerFotosAdicionales(Long mascotaId) {
        List<String> urls = new ArrayList<>();
        for (MascotaFoto foto : mascotaFotoRepository.findByMascotaIdOrderByOrdenAsc(mascotaId)) {
            urls.add(foto.getFotoUrl());
        }
        return urls;
    }

    // Solo se copian datos públicos del publicador: nunca passwordHash, correo ni teléfono
    private MascotaResponse toResponse(Mascota mascota, Usuario publicador, List<String> fotosAdicionales) {
        String publicadorNombre = null;
        String tipoPublicador = null;
        String ubicacionPublicador = null;
        String publicadorFotoUrl = null;
        if (publicador != null) {
            publicadorNombre = publicador.getNombre();
            tipoPublicador = publicador.getTipoPublicador();
            ubicacionPublicador = publicador.getUbicacion();
            publicadorFotoUrl = publicador.getFotoPerfilUrl();
        }

        return new MascotaResponse(
                mascota.getId(),
                mascota.getNombre(),
                mascota.getPublicadorId(),
                publicadorNombre,
                tipoPublicador,
                ubicacionPublicador,
                mascota.getEspecie(),
                mascota.getSexo(),
                mascota.getEdadAproximada(),
                mascota.getFotoUrl(),
                mascota.getTamano(),
                mascota.getNivelEnergia(),
                mascota.getDescripcion(),
                mascota.getEstado(),
                mascota.getFechaPublicacion(),
                publicadorFotoUrl,
                fotosAdicionales
        );
    }
}

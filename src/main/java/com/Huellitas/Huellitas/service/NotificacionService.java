package com.Huellitas.Huellitas.service;

import com.Huellitas.Huellitas.config.UsuarioAutenticado;
import com.Huellitas.Huellitas.dto.NotificacionItem;
import com.Huellitas.Huellitas.dto.NotificacionResumen;
import com.Huellitas.Huellitas.modelo.Constantes;
import com.Huellitas.Huellitas.modelo.Mascota;
import com.Huellitas.Huellitas.modelo.SolicitudAdopcion;
import com.Huellitas.Huellitas.repository.MascotaRepository;
import com.Huellitas.Huellitas.repository.SolicitudRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Notificaciones derivadas de los flags leidaPublicador / leidaAdoptante
 * de las solicitudes (no hay tabla de notificaciones).
 */
@Service
public class NotificacionService {

    private static final int MAX_ITEMS = 20;

    private final SolicitudRepository solicitudRepository;
    private final MascotaRepository mascotaRepository;

    public NotificacionService(SolicitudRepository solicitudRepository,
                               MascotaRepository mascotaRepository) {
        this.solicitudRepository = solicitudRepository;
        this.mascotaRepository = mascotaRepository;
    }

    // El administrador no recibe notificaciones
    public NotificacionResumen resumen(UsuarioAutenticado usuario) {
        if (Constantes.ROL_PUBLICADOR.equals(usuario.rol())) {
            return construir(solicitudRepository.findNoLeidasPorPublicador(usuario.id()), true);
        }
        if (Constantes.ROL_ADOPTANTE.equals(usuario.rol())) {
            return construir(
                    solicitudRepository.findByAdoptanteIdAndLeidaAdoptanteFalseOrderByFechaSolicitudDesc(usuario.id()),
                    false);
        }
        return new NotificacionResumen(0, new ArrayList<>());
    }

    // noLeidas ya viene ordenada de la más reciente a la más antigua
    private NotificacionResumen construir(List<SolicitudAdopcion> noLeidas, boolean paraPublicador) {
        List<SolicitudAdopcion> recientes = new ArrayList<>();
        for (int i = 0; i < noLeidas.size() && i < MAX_ITEMS; i++) {
            recientes.add(noLeidas.get(i));
        }

        Map<Long, String> nombresMascotas = buscarNombresMascotas(recientes);

        List<NotificacionItem> items = new ArrayList<>();
        for (SolicitudAdopcion s : recientes) {
            String mascota = nombresMascotas.getOrDefault(s.getMascotaId(), "una mascota");
            String mensaje;
            if (paraPublicador) {
                mensaje = mensajePublicador(s, mascota);
            } else {
                mensaje = mensajeAdoptante(s, mascota);
            }
            items.add(new NotificacionItem(s.getId(), s.getMascotaId(), mascota, mensaje, s.getFechaSolicitud()));
        }

        return new NotificacionResumen(noLeidas.size(), items);
    }

    // id de la mascota -> nombre, en una sola consulta
    private Map<Long, String> buscarNombresMascotas(List<SolicitudAdopcion> solicitudes) {
        List<Long> mascotaIds = new ArrayList<>();
        for (SolicitudAdopcion s : solicitudes) {
            if (!mascotaIds.contains(s.getMascotaId())) {
                mascotaIds.add(s.getMascotaId());
            }
        }

        Map<Long, String> nombres = new HashMap<>();
        for (Mascota mascota : mascotaRepository.findAllById(mascotaIds)) {
            nombres.put(mascota.getId(), mascota.getNombre());
        }
        return nombres;
    }

    private static String mensajePublicador(SolicitudAdopcion s, String mascota) {
        return s.getNombreContacto() + " envió una solicitud para " + mascota;
    }

    private static String mensajeAdoptante(SolicitudAdopcion s, String mascota) {
        if (Constantes.SOLICITUD_CONTACTADA.equals(s.getEstado())) {
            return "Quien publicó a " + mascota + " se puso en contacto contigo";
        }
        if (Constantes.SOLICITUD_CERRADA.equals(s.getEstado())) {
            return "Tu solicitud para " + mascota + " fue cerrada";
        }
        return "Tu solicitud para " + mascota + " volvió a estar en revisión";
    }
}

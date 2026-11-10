package com.Huellitas.Huellitas.service;

import com.Huellitas.Huellitas.config.UsuarioAutenticado;
import com.Huellitas.Huellitas.dto.CrearSolicitudRequest;
import com.Huellitas.Huellitas.modelo.Constantes;
import com.Huellitas.Huellitas.modelo.Mascota;
import com.Huellitas.Huellitas.modelo.SolicitudAdopcion;
import com.Huellitas.Huellitas.repository.MascotaFotoRepository;
import com.Huellitas.Huellitas.repository.MascotaRepository;
import com.Huellitas.Huellitas.repository.SolicitudRepository;
import com.Huellitas.Huellitas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * Al crear una solicitud se piden los dos correos (adoptante y publicador) y, al cambiarla a
 * CONTACTADA o CERRADA, el aviso al adoptante. EmailService es simulado: aquí solo se revisa
 * que SolicitudService lo llame; el envío real a Brevo corre en segundo plano (@Async).
 */
class SolicitudServiceCorreoTest {

    private static final UsuarioAutenticado ADOPTANTE = new UsuarioAutenticado(20L, "a@huellitas.test", Constantes.ROL_ADOPTANTE);
    private static final UsuarioAutenticado PUBLICADOR = new UsuarioAutenticado(7L, "p@huellitas.test", Constantes.ROL_PUBLICADOR);

    private EmailService emailService;
    private SolicitudRepository solicitudRepository;
    private SolicitudService service;

    @BeforeEach
    void preparar() {
        MascotaRepository mascotaRepository = mock(MascotaRepository.class);
        solicitudRepository = mock(SolicitudRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        emailService = mock(EmailService.class);

        Mascota mascota = new Mascota();
        mascota.setId(1L);
        mascota.setNombre("Rocky");
        mascota.setPublicadorId(PUBLICADOR.id());
        mascota.setEstado(Constantes.MASCOTA_DISPONIBLE);
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(solicitudRepository.saveAndFlush(any(SolicitudAdopcion.class))).thenAnswer(inv -> {
            SolicitudAdopcion s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        MascotaService mascotaService = new MascotaService(mascotaRepository, usuarioRepository,
                solicitudRepository, mock(MascotaFotoRepository.class),
                "https://ejemplo.supabase.co/", "mascotas");
        service = new SolicitudService(solicitudRepository, mascotaRepository, usuarioRepository,
                mascotaService, emailService);
    }

    private static CrearSolicitudRequest pedido() {
        return new CrearSolicitudRequest(1L, "Ana López", "5512345678", "ana@correo.com",
                null, null, null, null, null, null);
    }

    private SolicitudAdopcion existeSolicitud(String estado) {
        SolicitudAdopcion solicitud = new SolicitudAdopcion();
        solicitud.setId(100L);
        solicitud.setMascotaId(1L);
        solicitud.setAdoptanteId(ADOPTANTE.id());
        solicitud.setEstado(estado);
        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud));
        return solicitud;
    }

    @Test
    void crearPideLosDosCorreos() {
        service.crear(pedido(), ADOPTANTE);
        verify(emailService).enviarConfirmacionAdoptante(any(SolicitudAdopcion.class));
        verify(emailService).enviarSolicitudPublicador(any(SolicitudAdopcion.class));
    }

    @Test
    void laRespuestaDiceSiElCorreoVaEnCamino() {
        when(emailService.estaActivo()).thenReturn(true);
        assertEquals(Boolean.TRUE, service.crear(pedido(), ADOPTANTE).correoConfirmacionEnviado());

        when(emailService.estaActivo()).thenReturn(false);
        assertEquals(Boolean.FALSE, service.crear(pedido(), ADOPTANTE).correoConfirmacionEnviado());
    }

    @Test
    void otrasRespuestasNoIncluyenElAviso() {
        SolicitudAdopcion solicitud = existeSolicitud(Constantes.SOLICITUD_PENDIENTE);
        assertNull(service.toResponse(solicitud).correoConfirmacionEnviado());
    }

    @Test
    void contactadaYCerradaAvisanAlAdoptante() {
        existeSolicitud(Constantes.SOLICITUD_PENDIENTE);
        service.cambiarEstado(100L, Constantes.SOLICITUD_CONTACTADA, PUBLICADOR);
        verify(emailService).enviarCambioEstadoAdoptante(any(SolicitudAdopcion.class));
    }

    @Test
    void volverAPendienteNoAvisa() {
        existeSolicitud(Constantes.SOLICITUD_CONTACTADA);
        service.cambiarEstado(100L, Constantes.SOLICITUD_PENDIENTE, PUBLICADOR);
        verify(emailService, never()).enviarCambioEstadoAdoptante(any(SolicitudAdopcion.class));
    }
}

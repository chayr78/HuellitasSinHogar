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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * Al crear una solicitud se le envía un correo de confirmación al adoptante, al correo que escribió
 * en el formulario. La respuesta dice si salió, para que el frontend no prometa un correo que no llegó.
 */
class SolicitudServiceCorreoTest {

    private static final UsuarioAutenticado ADOPTANTE = new UsuarioAutenticado(20L, "a@huellitas.test", Constantes.ROL_ADOPTANTE);

    private EmailService emailService;
    private SolicitudService service;

    @BeforeEach
    void preparar() {
        MascotaRepository mascotaRepository = mock(MascotaRepository.class);
        SolicitudRepository solicitudRepository = mock(SolicitudRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        emailService = mock(EmailService.class);

        Mascota mascota = new Mascota();
        mascota.setId(1L);
        mascota.setNombre("Rocky");
        mascota.setPublicadorId(7L);
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

    @Test
    void siElCorreoSaleLaRespuestaLoDice() {
        when(emailService.enviar(eq("ana@correo.com"), anyString(), anyString())).thenReturn(true);
        assertEquals(Boolean.TRUE, service.crear(pedido(), ADOPTANTE).correoConfirmacionEnviado());
        verify(emailService).enviar(eq("ana@correo.com"), eq("Recibimos tu solicitud para adoptar a Rocky"), anyString());
    }

    @Test
    void siElCorreoNoSaleLaSolicitudSeCreaIgual() {
        when(emailService.enviar(anyString(), anyString(), anyString())).thenReturn(false);
        assertEquals(Boolean.FALSE, service.crear(pedido(), ADOPTANTE).correoConfirmacionEnviado());
    }

    @Test
    void siEnviarLanzaExcepcionLaSolicitudSeCreaIgual() {
        when(emailService.enviar(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("caído"));
        assertEquals(Boolean.FALSE, service.crear(pedido(), ADOPTANTE).correoConfirmacionEnviado());
    }

    @Test
    void otrasRespuestasNoIncluyenElAviso() {
        SolicitudAdopcion solicitud = new SolicitudAdopcion();
        solicitud.setId(100L);
        solicitud.setMascotaId(1L);
        solicitud.setAdoptanteId(ADOPTANTE.id());
        assertNull(service.toResponse(solicitud).correoConfirmacionEnviado());
    }
}

package com.Huellitas.Huellitas.service;

import com.Huellitas.Huellitas.config.UsuarioAutenticado;
import com.Huellitas.Huellitas.dto.CrearSolicitudRequest;
import com.Huellitas.Huellitas.dto.SolicitudResponse;
import com.Huellitas.Huellitas.modelo.Constantes;
import com.Huellitas.Huellitas.modelo.Mascota;
import com.Huellitas.Huellitas.modelo.SolicitudAdopcion;
import com.Huellitas.Huellitas.modelo.Usuario;
import com.Huellitas.Huellitas.repository.MascotaFotoRepository;
import com.Huellitas.Huellitas.repository.MascotaRepository;
import com.Huellitas.Huellitas.repository.SolicitudRepository;
import com.Huellitas.Huellitas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
 * Al crear una solicitud, el adoptante recibe el WhatsApp (teléfono solo con dígitos) del publicador.
 * En las demás respuestas, por ejemplo la lista que ve el publicador, nunca va.
 */
class SolicitudServiceWhatsappTest {

    private static final UsuarioAutenticado ADOPTANTE = new UsuarioAutenticado(20L, "a@huellitas.test", Constantes.ROL_ADOPTANTE);
    private static final long PUBLICADOR_ID = 7L;

    private UsuarioRepository usuarioRepository;
    private SolicitudRepository solicitudRepository;
    private SolicitudService service;

    @BeforeEach
    void preparar() {
        MascotaRepository mascotaRepository = mock(MascotaRepository.class);
        solicitudRepository = mock(SolicitudRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);

        Mascota mascota = new Mascota();
        mascota.setId(1L);
        mascota.setPublicadorId(PUBLICADOR_ID);
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
                mascotaService, mock(EmailService.class));
    }

    private void publicadorConTelefono(String telefono) {
        Usuario publicador = new Usuario();
        publicador.setId(PUBLICADOR_ID);
        publicador.setNombre("Refugio Patitas");
        publicador.setCorreo("p@huellitas.test");
        publicador.setTelefono(telefono);
        when(usuarioRepository.findById(PUBLICADOR_ID)).thenReturn(Optional.of(publicador));
    }

    private static CrearSolicitudRequest pedido() {
        return new CrearSolicitudRequest(1L, "Ana López", "5512345678", "ana@correo.com",
                null, null, null, null, null, null);
    }

    @Test
    void crearIncluyeElWhatsappDelPublicadorSoloConDigitos() {
        publicadorConTelefono("+52 55 1234-5678");
        SolicitudResponse respuesta = service.crear(pedido(), ADOPTANTE);
        assertEquals("525512345678", respuesta.whatsappPublicador());
    }

    @Test
    void publicadorSinTelefonoDaNull() {
        publicadorConTelefono(null);
        SolicitudResponse respuesta = service.crear(pedido(), ADOPTANTE);
        assertNull(respuesta.whatsappPublicador());
    }

    @Test
    void otrasRespuestasNoIncluyenElWhatsapp() {
        publicadorConTelefono("5512345678");
        SolicitudAdopcion solicitud = new SolicitudAdopcion();
        solicitud.setId(100L);
        solicitud.setMascotaId(1L);
        solicitud.setAdoptanteId(ADOPTANTE.id());
        assertNull(service.toResponse(solicitud).whatsappPublicador());
        assertNull(service.toResponse(List.of(solicitud)).get(0).whatsappPublicador());
    }
}

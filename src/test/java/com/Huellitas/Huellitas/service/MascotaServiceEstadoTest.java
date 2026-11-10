package com.Huellitas.Huellitas.service;

import com.Huellitas.Huellitas.config.UsuarioAutenticado;
import com.Huellitas.Huellitas.exception.ApiException;
import com.Huellitas.Huellitas.modelo.Constantes;
import com.Huellitas.Huellitas.modelo.Mascota;
import com.Huellitas.Huellitas.repository.MascotaFotoRepository;
import com.Huellitas.Huellitas.repository.MascotaRepository;
import com.Huellitas.Huellitas.repository.SolicitudRepository;
import com.Huellitas.Huellitas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
 * Una mascota adoptada no puede volver a DISPONIBLE ni a EN_PROCESO, ni siquiera por un administrador.
 */
class MascotaServiceEstadoTest {

    private static final UsuarioAutenticado PUBLICADOR = new UsuarioAutenticado(7L, "p@huellitas.test", Constantes.ROL_PUBLICADOR);
    private static final UsuarioAutenticado ADMIN = new UsuarioAutenticado(1L, "a@huellitas.test", Constantes.ROL_ADMINISTRADOR);

    private MascotaRepository mascotaRepository;
    private MascotaService service;

    @BeforeEach
    void preparar() {
        mascotaRepository = mock(MascotaRepository.class);
        when(mascotaRepository.save(any(Mascota.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new MascotaService(mascotaRepository, mock(UsuarioRepository.class),
                mock(SolicitudRepository.class), mock(MascotaFotoRepository.class),
                "https://ejemplo.supabase.co/", "mascotas");
    }

    private void existeMascota(String estado) {
        Mascota mascota = new Mascota();
        mascota.setId(1L);
        mascota.setPublicadorId(PUBLICADOR.id());
        mascota.setEstado(estado);
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
    }

    @Test
    void adoptadaNoVuelveADisponible() {
        existeMascota(Constantes.MASCOTA_ADOPTADO);
        ApiException error = assertThrows(ApiException.class,
                () -> service.cambiarEstado(1L, Constantes.MASCOTA_DISPONIBLE, PUBLICADOR));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
    }

    @Test
    void adoptadaNoVuelveAEnProcesoNiConAdmin() {
        existeMascota(Constantes.MASCOTA_ADOPTADO);
        assertThrows(ApiException.class, () -> service.cambiarEstado(1L, Constantes.MASCOTA_EN_PROCESO, ADMIN));
    }

    @Test
    void disponiblePuedePasarAAdoptada() {
        existeMascota(Constantes.MASCOTA_DISPONIBLE);
        assertDoesNotThrow(() -> service.cambiarEstado(1L, Constantes.MASCOTA_ADOPTADO, PUBLICADOR));
    }
}

package com.Huellitas.Huellitas.service;

import com.Huellitas.Huellitas.config.UsuarioAutenticado;
import com.Huellitas.Huellitas.dto.MascotaRequest;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
 * Al crear o editar una mascota, sus fotos deben venir de POST /api/uploads/mascota.
 * Los repositorios son simulados (Mockito): no hace falta base de datos.
 */
class MascotaServiceFotosTest {

    private static final String BUCKET = "https://ejemplo.supabase.co/storage/v1/object/public/mascotas/";
    private static final String FOTO_PROPIA = BUCKET + "7/0f8fad5b-d9cb-469f-a165-70867728950e.jpg";
    private static final String FOTO_PROPIA_2 = BUCKET + "7/7c9e6679-7425-40de-944b-e07fc1f90ae7.webp";
    private static final UsuarioAutenticado PUBLICADOR = new UsuarioAutenticado(7L, "p@huellitas.test", Constantes.ROL_PUBLICADOR);

    private MascotaRepository mascotaRepository;
    private MascotaService service;

    @BeforeEach
    void preparar() {
        mascotaRepository = mock(MascotaRepository.class);
        when(mascotaRepository.save(any(Mascota.class))).thenAnswer(inv -> {
            Mascota m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId(1L);
            }
            return m;
        });
        service = new MascotaService(mascotaRepository, mock(UsuarioRepository.class),
                mock(SolicitudRepository.class), mock(MascotaFotoRepository.class),
                "https://ejemplo.supabase.co/", "mascotas");
    }

    private static MascotaRequest pedido(String fotoUrl, List<String> fotosAdicionales) {
        return new MascotaRequest("Firulais", "PERRO", "MACHO", "2 años", fotoUrl,
                "MEDIANO", "MEDIA", "Muy juguetón", fotosAdicionales);
    }

    private void assertFotoRechazada(Runnable accion) {
        ApiException error = assertThrows(ApiException.class, accion::run);
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals("Las fotos deben subirse desde el formulario.", error.getMessage());
    }

    @Test
    void crearConFotosSubidasFunciona() {
        assertDoesNotThrow(() -> service.crear(pedido(FOTO_PROPIA, List.of(FOTO_PROPIA_2)), PUBLICADOR));
    }

    @Test
    void crearSinFotoFunciona() {
        assertDoesNotThrow(() -> service.crear(pedido(null, null), PUBLICADOR));
    }

    @Test
    void urlExternaEnFotoUrlDa400() {
        assertFotoRechazada(() -> service.crear(pedido("https://otro-sitio.com/perro.jpg", null), PUBLICADOR));
    }

    @Test
    void urlExternaEnFotosAdicionalesDa400() {
        assertFotoRechazada(() -> service.crear(
                pedido(FOTO_PROPIA, List.of("https://otro-sitio.com/perro.jpg")), PUBLICADOR));
    }

    // La foto de perfil no pasa por la detección de mascotas, así que no puede usarse aquí
    @Test
    void fotoDePerfilDa400() {
        assertFotoRechazada(() -> service.crear(
                pedido(BUCKET + "perfiles/7/0f8fad5b-d9cb-469f-a165-70867728950e.jpg", null), PUBLICADOR));
    }

    @Test
    void fotoDeOtroPublicadorDa400() {
        assertFotoRechazada(() -> service.crear(
                pedido(BUCKET + "8/0f8fad5b-d9cb-469f-a165-70867728950e.jpg", null), PUBLICADOR));
    }

    @Test
    void rutaConPuntosDa400() {
        assertFotoRechazada(() -> service.crear(
                pedido(BUCKET + "7/../perfiles/7/0f8fad5b-d9cb-469f-a165-70867728950e.jpg", null), PUBLICADOR));
    }

    // Una mascota publicada antes de esta validación conserva su foto externa al editarse
    @Test
    void editarConservandoFotoAnteriorFunciona() {
        Mascota existente = mascotaExistente("https://otro-sitio.com/vieja.jpg");
        assertDoesNotThrow(() -> service.actualizar(1L, pedido("https://otro-sitio.com/vieja.jpg", null), PUBLICADOR));
        assertEquals("https://otro-sitio.com/vieja.jpg", existente.getFotoUrl());
    }

    @Test
    void editarConFotoExternaNuevaDa400() {
        mascotaExistente("https://otro-sitio.com/vieja.jpg");
        assertFotoRechazada(() -> service.actualizar(1L, pedido("https://otro-sitio.com/nueva.jpg", null), PUBLICADOR));
    }

    private Mascota mascotaExistente(String fotoUrl) {
        Mascota mascota = new Mascota();
        mascota.setId(1L);
        mascota.setPublicadorId(PUBLICADOR.id());
        mascota.setFotoUrl(fotoUrl);
        mascota.setEstado(Constantes.MASCOTA_DISPONIBLE);
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        return mascota;
    }
}

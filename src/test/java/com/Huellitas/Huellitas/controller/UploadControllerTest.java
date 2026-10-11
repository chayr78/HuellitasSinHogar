package com.Huellitas.Huellitas.controller;

import com.Huellitas.Huellitas.config.UsuarioAutenticado;
import com.Huellitas.Huellitas.exception.GlobalExceptionHandler;
import com.Huellitas.Huellitas.modelo.Constantes;
import com.Huellitas.Huellitas.service.DeteccionMascotaService;
import com.Huellitas.Huellitas.service.UploadService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * POST /api/uploads/mascota y /perfil con el modelo real. Supabase Storage se reemplaza
 * por un servidor HTTP local que solo cuenta cuántos archivos le llegan.
 */
class UploadControllerTest {

    private static final long PUBLICADOR_ID = 7L;

    private static DeteccionMascotaService deteccion;
    private static HttpServer supabaseFalso;
    private static final AtomicInteger archivosGuardados = new AtomicInteger();

    private MockMvc mvc;

    @BeforeAll
    static void iniciar() throws IOException {
        deteccion = new DeteccionMascotaService(0.30, true);
        supabaseFalso = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        supabaseFalso.createContext("/", intercambio -> {
            intercambio.getRequestBody().readAllBytes();
            archivosGuardados.incrementAndGet();
            intercambio.sendResponseHeaders(200, -1);
            intercambio.close();
        });
        supabaseFalso.start();
    }

    @AfterAll
    static void detener() throws Exception {
        supabaseFalso.stop(0);
        deteccion.cerrar();
    }

    @BeforeEach
    void preparar() {
        archivosGuardados.set(0);
        String url = "http://localhost:" + supabaseFalso.getAddress().getPort();
        UploadService uploadService = new UploadService(deteccion, url, "clave-de-prueba", "mascotas");
        mvc = MockMvcBuilders.standaloneSetup(new UploadController(uploadService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        UsuarioAutenticado usuario = new UsuarioAutenticado(PUBLICADOR_ID, "p@huellitas.test", Constantes.ROL_PUBLICADOR);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static MockMultipartFile archivo(String nombre) throws IOException {
        try (InputStream entrada = UploadControllerTest.class.getResourceAsStream("/imagenes/" + nombre)) {
            return new MockMultipartFile("archivo", nombre, "image/jpeg", entrada.readAllBytes());
        }
    }

    @Test
    void fotoDePerroSeSube() throws Exception {
        mvc.perform(multipart("/api/uploads/mascota").file(archivo("perro.jpg")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exito").value(true));
        assertEquals(1, archivosGuardados.get());
    }

    @Test
    void fotoDeGatoSeSube() throws Exception {
        mvc.perform(multipart("/api/uploads/mascota").file(archivo("gato.jpg")))
                .andExpect(status().isCreated());
        assertEquals(1, archivosGuardados.get());
    }

    @Test
    void fotoSinAnimalesDa422YNoSeGuarda() throws Exception {
        mvc.perform(multipart("/api/uploads/mascota").file(archivo("paisaje.jpg")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.exito").value(false))
                .andExpect(jsonPath("$.mensaje").value(
                        "No detectamos un perro o gato en la foto. Elige otra donde se vea bien a la mascota, con buena luz."));
        assertEquals(0, archivosGuardados.get());
    }

    @Test
    void fotoDePerfilNoSeRevisa() throws Exception {
        mvc.perform(multipart("/api/uploads/perfil").file(archivo("auto.jpg")))
                .andExpect(status().isCreated());
        assertEquals(1, archivosGuardados.get());
    }
}

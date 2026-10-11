package com.Huellitas.Huellitas.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Prueba el modelo real con fotos reales (src/test/resources/imagenes, de Wikimedia Commons).
 * No necesita base de datos ni Spring.
 */
class DeteccionMascotaServiceTest {

    private static DeteccionMascotaService deteccion;

    @BeforeAll
    static void cargarModelo() {
        deteccion = new DeteccionMascotaService(0.30, true);
    }

    @AfterAll
    static void cerrarModelo() throws Exception {
        deteccion.cerrar();
    }

    static byte[] imagen(String nombre) throws IOException {
        try (InputStream entrada = DeteccionMascotaServiceTest.class.getResourceAsStream("/imagenes/" + nombre)) {
            return entrada.readAllBytes();
        }
    }

    // perro.webp es un labrador negro sobre fondo negro: prueba WebP y una foto oscura
    @ParameterizedTest
    @ValueSource(strings = {"perro.jpg", "gato.jpg", "perro.webp"})
    void fotosConMascotaPasan(String nombre) throws Exception {
        byte[] foto = imagen(nombre);
        System.out.printf("%s -> probabilidad de mascota %.3f%n", nombre, deteccion.probabilidadMascota(foto));
        assertTrue(deteccion.contieneMascota(foto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"paisaje.jpg", "auto.jpg"})
    void fotosSinMascotaNoPasan(String nombre) throws Exception {
        byte[] foto = imagen(nombre);
        System.out.printf("%s -> probabilidad de mascota %.3f%n", nombre, deteccion.probabilidadMascota(foto));
        assertFalse(deteccion.contieneMascota(foto));
    }

    @Test
    void siFallaDecidePermitirSiFalla() throws Exception {
        byte[] noEsImagen = "esto no es una imagen".getBytes();
        assertTrue(deteccion.contieneMascota(noEsImagen));

        DeteccionMascotaService estricta = new DeteccionMascotaService(0.30, false);
        try {
            assertFalse(estricta.contieneMascota(noEsImagen));
        } finally {
            estricta.cerrar();
        }
    }
}

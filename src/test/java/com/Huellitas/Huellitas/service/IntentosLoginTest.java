package com.Huellitas.Huellitas.service;

import com.Huellitas.Huellitas.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 * Tras 5 intentos fallidos el correo queda bloqueado; un login correcto o una contraseña restablecida lo libera.
 */
class IntentosLoginTest {

    private static final String CORREO = "ana@correo.com";

    @Test
    void bloqueaDespuesDeCincoFallos() {
        IntentosLogin intentos = new IntentosLogin();
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> intentos.verificar(CORREO));
            intentos.registrarFallo(CORREO);
        }
        ApiException error = assertThrows(ApiException.class, () -> intentos.verificar(CORREO));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, error.getStatus());
        // Los demás correos no se ven afectados
        assertDoesNotThrow(() -> intentos.verificar("otro@correo.com"));
    }

    @Test
    void limpiarQuitaElBloqueo() {
        IntentosLogin intentos = new IntentosLogin();
        for (int i = 0; i < 5; i++) {
            intentos.registrarFallo(CORREO);
        }
        intentos.limpiar(CORREO);
        assertDoesNotThrow(() -> intentos.verificar(CORREO));
    }

    @Test
    void patronDeCorreoExigeTerminacion() {
        assertEquals(false, "ana@correo".matches(Textos.PATRON_CORREO));
        assertEquals(false, "ana@correo.c".matches(Textos.PATRON_CORREO));
        assertEquals(true, "ana@correo.com".matches(Textos.PATRON_CORREO));
        assertEquals(true, "ana.b@correo.com.mx".matches(Textos.PATRON_CORREO));
    }
}

package com.Huellitas.Huellitas.service;

import com.Huellitas.Huellitas.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Frena a quien prueba contraseñas: tras 5 intentos fallidos con el mismo correo,
 * ese correo no puede iniciar sesión durante 15 minutos (contados desde el primer fallo).
 * Se guarda en memoria: con una sola instancia en Railway es suficiente y al reiniciar se limpia.
 */
@Component
public class IntentosLogin {

    private static final int MAXIMO_FALLIDOS = 5;
    private static final Duration VENTANA = Duration.ofMinutes(15);
    // Por encima de esta cantidad de correos registrados se borran los vencidos, para no crecer sin límite
    private static final int LIMPIAR_DESDE = 10_000;

    private record Fallidos(int cantidad, Instant desde) {}

    // correo normalizado -> intentos fallidos dentro de la ventana
    private final Map<String, Fallidos> fallidos = new ConcurrentHashMap<>();

    // Lanza 429 si el correo ya agotó sus intentos
    public void verificar(String correo) {
        Fallidos registro = fallidos.get(correo);
        if (registro == null) {
            return;
        }
        if (vencido(registro)) {
            fallidos.remove(correo, registro);
            return;
        }
        if (registro.cantidad() >= MAXIMO_FALLIDOS) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Espera 15 minutos o restablece tu contraseña.");
        }
    }

    public void registrarFallo(String correo) {
        if (fallidos.size() > LIMPIAR_DESDE) {
            fallidos.values().removeIf(IntentosLogin::vencido);
        }
        fallidos.compute(correo, (clave, registro) -> registro == null || vencido(registro)
                ? new Fallidos(1, Instant.now())
                : new Fallidos(registro.cantidad() + 1, registro.desde()));
    }

    // Tras un login correcto o una contraseña restablecida
    public void limpiar(String correo) {
        fallidos.remove(correo);
    }

    private static boolean vencido(Fallidos registro) {
        return registro.desde().plus(VENTANA).isBefore(Instant.now());
    }
}

package com.Huellitas.Huellitas.dto;

import java.time.LocalDateTime;
import java.util.List;

/*
 * Respuesta de los endpoints de mascotas: los datos de la mascota más
 * los datos públicos de su publicador. Nunca incluye passwordHash,
 * correo ni teléfono del usuario.
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record MascotaResponse(
        Long id,
        String nombre,
        Long publicadorId,
        String publicadorNombre,
        String tipoPublicador,
        String ubicacionPublicador,
        String especie,
        String edadAproximada,
        String fotoUrl,
        String tamano,
        String nivelEnergia,
        String descripcion,
        String estado,
        LocalDateTime fechaPublicacion,
        String publicadorFotoUrl,
        // Fotos además de la portada, en orden. En los listados va vacía.
        List<String> fotosAdicionales
) {}

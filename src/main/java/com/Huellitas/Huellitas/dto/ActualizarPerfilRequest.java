package com.Huellitas.Huellitas.dto;

import com.Huellitas.Huellitas.service.Textos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*
 * Body de PUT /api/auth/me. Correo, rol y tipo de publicador no se pueden cambiar.
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record ActualizarPerfilRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
        @Pattern(regexp = Textos.PATRON_TELEFONO, message = Textos.MENSAJE_TELEFONO)
        String telefono,

        // El frontend envía el nombre de un municipio del Estado de México (p. ej. "Tecámac")
        @Size(max = 150, message = "La ubicación no puede superar 150 caracteres")
        String ubicacion,

        // El service verifica que sea una URL de nuestro Supabase Storage
        @Size(max = 255, message = "La URL de la foto no puede superar 255 caracteres")
        String fotoPerfilUrl
) {}

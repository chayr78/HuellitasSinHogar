package com.Huellitas.Huellitas.dto;

import com.Huellitas.Huellitas.service.Textos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*
 * Body de POST /api/auth/restablecer-password.
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record RestablecerPasswordRequest(
        @NotBlank(message = "El enlace de restablecimiento no es válido")
        String token,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        @Pattern(regexp = Textos.PATRON_PASSWORD, message = Textos.MENSAJE_PASSWORD)
        String nuevaPassword
) {}

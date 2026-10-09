package com.Huellitas.Huellitas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/*
 * Body de POST /api/auth/olvide-password.
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record OlvidePasswordRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String correo
) {}

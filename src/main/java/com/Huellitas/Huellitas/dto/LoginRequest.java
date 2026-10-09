package com.Huellitas.Huellitas.dto;

import jakarta.validation.constraints.NotBlank;

/*
 * Body de POST /api/auth/login.
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}

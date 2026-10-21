package com.Huellitas.Huellitas.dto;

import com.Huellitas.Huellitas.service.Textos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*
 * Body de POST /api/auth/registro.
 * tipoPublicador es obligatorio cuando rol = PUBLICADOR (se valida en AuthService).
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
        @Pattern(regexp = Textos.PATRON_TELEFONO, message = Textos.MENSAJE_TELEFONO)
        String telefono,

        @NotBlank(message = "El rol es obligatorio")
        @Pattern(regexp = "PUBLICADOR|ADOPTANTE", message = "El rol debe ser PUBLICADOR o ADOPTANTE")
        String rol,

        @Pattern(regexp = "REFUGIO|PARTICULAR", message = "El tipo de publicador debe ser REFUGIO o PARTICULAR")
        String tipoPublicador,

        @Size(max = 150, message = "La ubicación no puede superar 150 caracteres")
        String ubicacion
) {}

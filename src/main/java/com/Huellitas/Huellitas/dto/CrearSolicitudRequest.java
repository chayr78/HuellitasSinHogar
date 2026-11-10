package com.Huellitas.Huellitas.dto;

import com.Huellitas.Huellitas.service.Textos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*
 * Body de POST /api/solicitudes. El adoptante sale del token, nunca del body.
 * Los campos de la encuesta son opcionales (null si no se hizo).
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record CrearSolicitudRequest(
        @NotNull(message = "La mascota es obligatoria")
        Long mascotaId,

        @NotBlank(message = "El nombre de contacto es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        @Pattern(regexp = Textos.PATRON_NOMBRE, message = Textos.MENSAJE_NOMBRE)
        String nombreContacto,

        @NotBlank(message = "El teléfono de contacto es obligatorio")
        @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
        @Pattern(regexp = Textos.PATRON_TELEFONO, message = Textos.MENSAJE_TELEFONO)
        String telefonoContacto,

        @NotBlank(message = "El correo de contacto es obligatorio")
        @Email(message = "El correo de contacto no tiene un formato válido")
        @Pattern(regexp = Textos.PATRON_CORREO, message = "El correo de contacto no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String correoContacto,

        @Size(max = 2000, message = "El mensaje no puede superar 2000 caracteres")
        String mensaje,

        @Pattern(regexp = "DEPTO_SIN_PATIO|PATIO_PEQUENO|PATIO_GRANDE",
                message = "El tipo de vivienda debe ser DEPTO_SIN_PATIO, PATIO_PEQUENO o PATIO_GRANDE")
        String tipoVivienda,

        @Pattern(regexp = "MENOS_2H|DE_2_A_5H|MAS_5H",
                message = "Las horas disponibles deben ser MENOS_2H, DE_2_A_5H o MAS_5H")
        String horasDisponibles,

        Boolean experienciaPrevia,
        Boolean ninosPequenos,
        Boolean otrasMascotas
) {}

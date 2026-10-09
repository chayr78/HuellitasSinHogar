package com.Huellitas.Huellitas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/*
 * Body de POST /api/solicitudes/compatibilidad.
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record CompatibilidadRequest(
        @NotNull(message = "La mascota es obligatoria")
        Long mascotaId,

        @NotBlank(message = "El tipo de vivienda es obligatorio")
        @Pattern(regexp = "DEPTO_SIN_PATIO|PATIO_PEQUENO|PATIO_GRANDE",
                message = "El tipo de vivienda debe ser DEPTO_SIN_PATIO, PATIO_PEQUENO o PATIO_GRANDE")
        String tipoVivienda,

        @NotBlank(message = "Las horas disponibles son obligatorias")
        @Pattern(regexp = "MENOS_2H|DE_2_A_5H|MAS_5H",
                message = "Las horas disponibles deben ser MENOS_2H, DE_2_A_5H o MAS_5H")
        String horasDisponibles
) {}

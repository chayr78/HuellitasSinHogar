package com.Huellitas.Huellitas.dto;

import jakarta.validation.constraints.NotBlank;

/*
 * Body de PATCH .../{id}/estado para mascotas y solicitudes.
 * Los valores permitidos se validan en cada service.
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record CambioEstadoRequest(
        @NotBlank(message = "El estado es obligatorio")
        String estado
) {}

package com.Huellitas.Huellitas.dto;

/*
 * Resultado de POST /api/solicitudes/compatibilidad.
 * advertencia = true cuando el porcentaje es menor a 75.
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record CompatibilidadResponse(
        short porcentaje,
        boolean advertencia
) {}

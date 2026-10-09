package com.Huellitas.Huellitas.dto;

import jakarta.validation.constraints.NotNull;

/*
 * Body de PATCH /api/admin/usuarios/{id}/estado.
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record CambioActivoRequest(
        @NotNull(message = "Indica si la cuenta queda activa (true) o desactivada (false)")
        Boolean activo
) {}

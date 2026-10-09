package com.Huellitas.Huellitas.dto;

import java.time.LocalDateTime;

/*
 * Una notificación dentro de NotificacionResumen.
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record NotificacionItem(
        Long solicitudId,
        Long mascotaId,
        String mascotaNombre,
        String mensaje,
        LocalDateTime fecha
) {}

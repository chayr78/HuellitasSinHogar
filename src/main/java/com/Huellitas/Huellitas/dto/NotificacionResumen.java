package com.Huellitas.Huellitas.dto;

import java.util.List;

/*
 * Respuesta de GET /api/notificaciones/resumen.
 * noLeidas es el total; items trae como máximo las 20 más recientes.
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record NotificacionResumen(long noLeidas, List<NotificacionItem> items) {}

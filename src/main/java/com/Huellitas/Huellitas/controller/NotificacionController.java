package com.Huellitas.Huellitas.controller;

import com.Huellitas.Huellitas.config.UsuarioActual;
import com.Huellitas.Huellitas.dto.ApiResponse;
import com.Huellitas.Huellitas.dto.NotificacionResumen;
import com.Huellitas.Huellitas.service.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * Endpoint con el resumen de notificaciones sin leer del usuario.
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    // GET /api/notificaciones/resumen
    @GetMapping("/resumen")
    public ResponseEntity<ApiResponse<NotificacionResumen>> resumen() {
        NotificacionResumen resumen = notificacionService.resumen(UsuarioActual.obtener());
        return ResponseEntity.ok(ApiResponse.ok(resumen.noLeidas() + " notificación(es) sin leer", resumen));
    }
}

package com.Huellitas.Huellitas.controller;

import com.Huellitas.Huellitas.config.UsuarioActual;
import com.Huellitas.Huellitas.dto.ApiResponse;
import com.Huellitas.Huellitas.dto.CambioEstadoRequest;
import com.Huellitas.Huellitas.dto.CompatibilidadRequest;
import com.Huellitas.Huellitas.dto.CompatibilidadResponse;
import com.Huellitas.Huellitas.dto.CrearSolicitudRequest;
import com.Huellitas.Huellitas.dto.SolicitudResponse;
import com.Huellitas.Huellitas.service.SolicitudService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * Endpoints de solicitudes de adopción y de la encuesta de compatibilidad.
 * Las reglas están en SolicitudService.
 */
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    // GET /api/solicitudes/mias  (historial del adoptante)
    @GetMapping("/mias")
    public ResponseEntity<ApiResponse<List<SolicitudResponse>>> obtenerMias() {
        return ResponseEntity.ok(
                ApiResponse.ok("Tus solicitudes", solicitudService.obtenerMias(UsuarioActual.obtener()))
        );
    }

    // GET /api/solicitudes/activa  (RF-13: advertencia de solicitud duplicada)
    @GetMapping("/activa")
    public ResponseEntity<ApiResponse<Boolean>> tieneSolicitudActiva() {
        boolean activa = solicitudService.tieneSolicitudActiva(UsuarioActual.obtener());
        String mensaje = "No tienes solicitudes activas";
        if (activa) {
            mensaje = "Ya tienes una solicitud pendiente o contactada";
        }
        return ResponseEntity.ok(ApiResponse.ok(mensaje, activa));
    }

    // GET /api/solicitudes/mascota/{mascotaId}  (RF-06: solicitudes recibidas por el publicador)
    @GetMapping("/mascota/{mascotaId}")
    public ResponseEntity<ApiResponse<List<SolicitudResponse>>> obtenerPorMascota(@PathVariable Long mascotaId) {
        List<SolicitudResponse> solicitudes = solicitudService.obtenerPorMascota(mascotaId, UsuarioActual.obtener());
        return ResponseEntity.ok(ApiResponse.ok("Solicitudes de la mascota " + mascotaId, solicitudes));
    }

    // POST /api/solicitudes  (RF-10 + RF-12)
    @PostMapping
    public ResponseEntity<ApiResponse<SolicitudResponse>> crearSolicitud(
            @Valid @RequestBody CrearSolicitudRequest body) {

        SolicitudResponse nueva = solicitudService.crear(body, UsuarioActual.obtener());

        String mensaje = "Solicitud enviada exitosamente";
        if (nueva.porcentajeCompatibilidad() != null) {
            mensaje = "Solicitud enviada. Compatibilidad: " + nueva.porcentajeCompatibilidad() + "%";
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(mensaje, nueva));
    }

    // POST /api/solicitudes/compatibilidad  (público: calcula el porcentaje sin crear la solicitud)
    // Body esperado: { "mascotaId": 1, "tipoVivienda": "PATIO_PEQUENO", "horasDisponibles": "DE_2_A_5H" }
    @PostMapping("/compatibilidad")
    public ResponseEntity<ApiResponse<CompatibilidadResponse>> calcularCompatibilidad(
            @Valid @RequestBody CompatibilidadRequest body) {
        CompatibilidadResponse resultado = solicitudService.calcularCompatibilidad(body);
        return ResponseEntity.ok(ApiResponse.ok("Compatibilidad: " + resultado.porcentaje() + "%", resultado));
    }

    // PATCH /api/solicitudes/{id}/estado  (RF-06)
    // Body esperado: { "estado": "CONTACTADA" }
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<SolicitudResponse>> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoRequest body) {
        SolicitudResponse actualizada = solicitudService.cambiarEstado(id, body.estado(), UsuarioActual.obtener());
        return ResponseEntity.ok(ApiResponse.ok("Estado actualizado a " + actualizada.estado(), actualizada));
    }

    // PATCH /api/solicitudes/{id}/leida-publicador
    @PatchMapping("/{id}/leida-publicador")
    public ResponseEntity<ApiResponse<SolicitudResponse>> marcarLeidaPublicador(@PathVariable Long id) {
        SolicitudResponse resultado = solicitudService.marcarLeidaPorPublicador(id, UsuarioActual.obtener());
        return ResponseEntity.ok(ApiResponse.ok("Marcada como leída por el publicador", resultado));
    }

    // PATCH /api/solicitudes/{id}/leida-adoptante
    @PatchMapping("/{id}/leida-adoptante")
    public ResponseEntity<ApiResponse<SolicitudResponse>> marcarLeidaAdoptante(@PathVariable Long id) {
        SolicitudResponse resultado = solicitudService.marcarLeidaPorAdoptante(id, UsuarioActual.obtener());
        return ResponseEntity.ok(ApiResponse.ok("Marcada como leída por el adoptante", resultado));
    }
}

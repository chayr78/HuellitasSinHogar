package com.Huellitas.Huellitas.controller;

import com.Huellitas.Huellitas.config.UsuarioActual;
import com.Huellitas.Huellitas.dto.ApiResponse;
import com.Huellitas.Huellitas.dto.CambioEstadoRequest;
import com.Huellitas.Huellitas.dto.MascotaRequest;
import com.Huellitas.Huellitas.dto.MascotaResponse;
import com.Huellitas.Huellitas.service.MascotaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * Endpoints de mascotas: catálogo público y gestión de publicaciones.
 * Las reglas (quién puede editar, estados válidos) están en MascotaService.
 */
@RestController
@RequestMapping("/api/mascotas")
public class MascotaController {

    private final MascotaService mascotaService;

    public MascotaController(MascotaService mascotaService) {
        this.mascotaService = mascotaService;
    }

    // GET /api/mascotas/disponibles  (catálogo público, RF-07)
    @GetMapping("/disponibles")
    public ResponseEntity<ApiResponse<List<MascotaResponse>>> obtenerDisponibles() {
        return ResponseEntity.ok(
                ApiResponse.ok("Mascotas disponibles para adopción", mascotaService.obtenerDisponibles())
        );
    }

    // GET /api/mascotas/adoptados  (historias felices)
    @GetMapping("/adoptados")
    public ResponseEntity<ApiResponse<List<MascotaResponse>>> obtenerAdoptados() {
        return ResponseEntity.ok(
                ApiResponse.ok("Mascotas adoptadas", mascotaService.obtenerAdoptados())
        );
    }

    // GET /api/mascotas/mias  (panel del publicador)
    @GetMapping("/mias")
    public ResponseEntity<ApiResponse<List<MascotaResponse>>> obtenerMias() {
        Long publicadorId = UsuarioActual.obtener().id();
        return ResponseEntity.ok(
                ApiResponse.ok("Tus mascotas publicadas", mascotaService.obtenerDelPublicador(publicadorId))
        );
    }

    // GET /api/mascotas/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MascotaResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Mascota encontrada", mascotaService.obtenerPorId(id)));
    }

    // POST /api/mascotas  (RF-03: publicar mascota)
    @PostMapping
    public ResponseEntity<ApiResponse<MascotaResponse>> crearMascota(@Valid @RequestBody MascotaRequest body) {
        MascotaResponse nueva = mascotaService.crear(body, UsuarioActual.obtener());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Mascota publicada exitosamente", nueva));
    }

    // PUT /api/mascotas/{id}  (RF-04: editar publicación)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MascotaResponse>> actualizarMascota(
            @PathVariable Long id,
            @Valid @RequestBody MascotaRequest body) {
        MascotaResponse actualizada = mascotaService.actualizar(id, body, UsuarioActual.obtener());
        return ResponseEntity.ok(ApiResponse.ok("Mascota actualizada correctamente", actualizada));
    }

    // PATCH /api/mascotas/{id}/estado  (RF-05: cambiar estado disponible/en_proceso/adoptado)
    // Body esperado: { "estado": "ADOPTADO" }
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<MascotaResponse>> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoRequest body) {
        MascotaResponse actualizada = mascotaService.cambiarEstado(id, body.estado(), UsuarioActual.obtener());
        return ResponseEntity.ok(ApiResponse.ok("Estado actualizado a " + actualizada.estado(), actualizada));
    }

    // DELETE /api/mascotas/{id}  (también elimina sus solicitudes)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarMascota(@PathVariable Long id) {
        mascotaService.eliminar(id, UsuarioActual.obtener());
        return ResponseEntity.ok(ApiResponse.ok("Mascota con id " + id + " eliminada correctamente"));
    }
}

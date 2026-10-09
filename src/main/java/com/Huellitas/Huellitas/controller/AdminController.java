package com.Huellitas.Huellitas.controller;

import com.Huellitas.Huellitas.config.UsuarioActual;
import com.Huellitas.Huellitas.dto.ApiResponse;
import com.Huellitas.Huellitas.dto.CambioActivoRequest;
import com.Huellitas.Huellitas.dto.MascotaResponse;
import com.Huellitas.Huellitas.dto.UsuarioResponse;
import com.Huellitas.Huellitas.service.AdminService;
import com.Huellitas.Huellitas.service.MascotaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * Endpoints exclusivos del administrador (moderación de usuarios y publicaciones).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final MascotaService mascotaService;

    public AdminController(AdminService adminService, MascotaService mascotaService) {
        this.adminService = adminService;
        this.mascotaService = mascotaService;
    }

    // GET /api/admin/usuarios  (RF-15)
    @GetMapping("/usuarios")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> obtenerUsuarios() {
        List<UsuarioResponse> usuarios = adminService.obtenerUsuarios();
        return ResponseEntity.ok(ApiResponse.ok("Se encontraron " + usuarios.size() + " usuario(s)", usuarios));
    }

    // GET /api/admin/mascotas  (todas, en cualquier estado)
    @GetMapping("/mascotas")
    public ResponseEntity<ApiResponse<List<MascotaResponse>>> obtenerMascotas() {
        List<MascotaResponse> mascotas = mascotaService.obtenerTodas();
        return ResponseEntity.ok(ApiResponse.ok("Se encontraron " + mascotas.size() + " mascota(s)", mascotas));
    }

    // PATCH /api/admin/usuarios/{id}/estado  (RF-16)
    // Body esperado: { "activo": false }
    @PatchMapping("/usuarios/{id}/estado")
    public ResponseEntity<ApiResponse<UsuarioResponse>> cambiarEstadoUsuario(
            @PathVariable Long id,
            @Valid @RequestBody CambioActivoRequest body) {
        UsuarioResponse usuario = adminService.cambiarActivo(id, body.activo(), UsuarioActual.obtener());
        String mensaje = "Cuenta desactivada";
        if (usuario.activo()) {
            mensaje = "Cuenta reactivada";
        }
        return ResponseEntity.ok(ApiResponse.ok(mensaje, usuario));
    }

    // DELETE /api/admin/mascotas/{id}  (elimina la mascota y sus solicitudes)
    @DeleteMapping("/mascotas/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarMascota(@PathVariable Long id) {
        mascotaService.eliminar(id, UsuarioActual.obtener());
        return ResponseEntity.ok(ApiResponse.ok("Publicación eliminada junto con sus solicitudes"));
    }
}

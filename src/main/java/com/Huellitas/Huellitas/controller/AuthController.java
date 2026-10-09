package com.Huellitas.Huellitas.controller;

import com.Huellitas.Huellitas.dto.ActualizarPerfilRequest;
import com.Huellitas.Huellitas.dto.ApiResponse;
import com.Huellitas.Huellitas.dto.AuthResponse;
import com.Huellitas.Huellitas.dto.LoginRequest;
import com.Huellitas.Huellitas.dto.OlvidePasswordRequest;
import com.Huellitas.Huellitas.dto.RegistroRequest;
import com.Huellitas.Huellitas.dto.RestablecerPasswordRequest;
import com.Huellitas.Huellitas.dto.UsuarioResponse;
import com.Huellitas.Huellitas.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/*
 * Endpoints de cuenta: registro, login, usuario actual y contraseña olvidada.
 * Solo reciben la petición y delegan en AuthService.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/auth/registro  (RF-01) -> queda logueado
    @PostMapping("/registro")
    public ResponseEntity<ApiResponse<AuthResponse>> registrar(@Valid @RequestBody RegistroRequest body) {
        AuthResponse respuesta = authService.registrar(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Cuenta creada exitosamente", respuesta));
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest body) {
        return ResponseEntity.ok(ApiResponse.ok("Sesión iniciada", authService.login(body)));
    }

    // GET /api/auth/me
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UsuarioResponse>> me() {
        return ResponseEntity.ok(ApiResponse.ok("Usuario autenticado", authService.me()));
    }

    // PUT /api/auth/me  (editar el propio perfil)
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizarPerfil(
            @Valid @RequestBody ActualizarPerfilRequest body) {
        return ResponseEntity.ok(ApiResponse.ok("Perfil actualizado", authService.actualizarPerfil(body)));
    }

    // POST /api/auth/olvide-password  (siempre responde lo mismo)
    @PostMapping("/olvide-password")
    public ResponseEntity<ApiResponse<Void>> olvidePassword(@Valid @RequestBody OlvidePasswordRequest body) {
        authService.olvidePassword(body);
        return ResponseEntity.ok(ApiResponse.ok("Si el correo existe, te enviamos instrucciones"));
    }

    // POST /api/auth/restablecer-password
    @PostMapping("/restablecer-password")
    public ResponseEntity<ApiResponse<Void>> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest body) {
        authService.restablecerPassword(body);
        return ResponseEntity.ok(ApiResponse.ok("Tu contraseña se actualizó. Ya puedes iniciar sesión."));
    }
}

package com.Huellitas.Huellitas.dto;

/*
 * Respuesta de registro y login: token JWT y datos del usuario.
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record AuthResponse(String token, UsuarioResponse usuario) {}

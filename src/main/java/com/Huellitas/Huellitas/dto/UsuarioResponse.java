package com.Huellitas.Huellitas.dto;

import com.Huellitas.Huellitas.modelo.Usuario;

import java.time.LocalDateTime;

/*
 * Datos de un usuario que se pueden devolver en la API.
 * Nunca incluye passwordHash ni los campos reset_token_*.
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        String telefono,
        String rol,
        String tipoPublicador,
        String ubicacion,
        Boolean activo,
        LocalDateTime fechaRegistro,
        String fotoPerfilUrl
) {
    public static UsuarioResponse desde(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getNombre(),
                u.getCorreo(),
                u.getTelefono(),
                u.getRol(),
                u.getTipoPublicador(),
                u.getUbicacion(),
                u.getActivo(),
                u.getFechaRegistro(),
                u.getFotoPerfilUrl()
        );
    }
}

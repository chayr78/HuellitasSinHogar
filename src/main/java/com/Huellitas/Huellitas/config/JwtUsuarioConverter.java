package com.Huellitas.Huellitas.config;

import com.Huellitas.Huellitas.modelo.Usuario;
import com.Huellitas.Huellitas.repository.UsuarioRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/*
 * Convierte un JWT válido en la autenticación de la petición.
 * En cada petición comprueba en la base que el usuario siga existiendo
 * y activo, para que una cuenta dada de baja pierda el acceso al momento.
 */
@Component
public class JwtUsuarioConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarioRepository;

    public JwtUsuarioConverter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // El "subject" del token es el id del usuario
        Long id;
        try {
            id = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new BadCredentialsException("Token inválido");
        }

        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null || !usuario.getActivo()) {
            throw new BadCredentialsException("Usuario inexistente o desactivado");
        }

        // El rol se toma de la base, no del token, por si cambió después de emitirlo.
        // Spring Security espera los roles con el prefijo "ROLE_".
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario.getId(), usuario.getCorreo(), usuario.getRol());
        SimpleGrantedAuthority rol = new SimpleGrantedAuthority("ROLE_" + usuario.getRol());
        return UsernamePasswordAuthenticationToken.authenticated(principal, jwt, List.of(rol));
    }
}

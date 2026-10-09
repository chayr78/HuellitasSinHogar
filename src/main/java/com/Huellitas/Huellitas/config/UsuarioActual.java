package com.Huellitas.Huellitas.config;

import com.Huellitas.Huellitas.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/*
 * Acceso al usuario autenticado desde controllers y services.
 */
public final class UsuarioActual {

    private UsuarioActual() {}

    // Lanza 401 si la petición no trae un token válido
    public static UsuarioAutenticado obtener() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return usuario;
        }
        throw ApiException.noAutenticado("Debes iniciar sesión");
    }
}

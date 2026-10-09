package com.Huellitas.Huellitas.config;

import com.Huellitas.Huellitas.modelo.Constantes;

/*
 * Usuario de la petición actual, obtenido del token JWT y verificado
 * contra la base de datos (existe y está activo).
 *
 * Es un "record": una clase de solo datos. Java crea automáticamente el
 * constructor y los métodos id(), correo() y rol().
 */
public record UsuarioAutenticado(Long id, String correo, String rol) {

    public boolean esAdmin() {
        return Constantes.ROL_ADMINISTRADOR.equals(rol);
    }
}

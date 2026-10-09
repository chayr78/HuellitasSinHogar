package com.Huellitas.Huellitas.service;

/*
 * Ayudas para limpiar los textos que llegan en las peticiones.
 */
public final class Textos {

    private Textos() {}

    // Campo opcional: vacío o solo espacios se guarda como null; si no, sin espacios en los extremos
    public static String textoOpcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}

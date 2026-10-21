package com.Huellitas.Huellitas.service;

/*
 * Ayudas para limpiar los textos que llegan en las peticiones.
 */
public final class Textos {

    private Textos() {}

    // Nombres: letras (con acentos y ñ), espacios, punto, apóstrofo y guion; al menos dos letras y sin números
    public static final String PATRON_NOMBRE = "^(?=(?:[^\\p{L}]*\\p{L}){2})[\\p{L}\\p{M}\\s.'-]+$";
    public static final String MENSAJE_NOMBRE = "El nombre solo puede tener letras, sin números ni símbolos";
    // Los refugios sí pueden llevar números en el nombre (p. ej. "Refugio Patitas 24")
    public static final String PATRON_NOMBRE_REFUGIO = "^(?=(?:[^\\p{L}]*\\p{L}){2})[\\p{L}\\p{M}\\d\\s.'-]+$";
    public static final String MENSAJE_NOMBRE_REFUGIO = "El nombre del refugio solo puede tener letras, números, espacios, puntos, apóstrofos o guiones";

    // Teléfonos: de 10 a 15 dígitos con + inicial opcional, espacios, guiones o paréntesis. Vacío se permite
    // (los campos obligatorios ya llevan @NotBlank)
    public static final String PATRON_TELEFONO = "^\\s*$|^(?=(?:\\D*\\d){10,15}\\D*$)\\s*\\+?[\\d\\s()-]+$";
    public static final String MENSAJE_TELEFONO = "El teléfono debe tener entre 10 y 15 dígitos y solo números";

    // Campo opcional: vacío o solo espacios se guarda como null; si no, sin espacios en los extremos
    public static String textoOpcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}

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

    // Teléfonos: de 10 a 15 dígitos con + inicial opcional, espacios, guiones o paréntesis, y no todos en cero.
    // Vacío se permite (los campos obligatorios ya llevan @NotBlank)
    public static final String PATRON_TELEFONO = "^\\s*$|^(?=(?:\\D*\\d){10,15}\\D*$)(?=[^1-9]*[1-9])\\s*\\+?[\\d\\s()-]+$";
    public static final String MENSAJE_TELEFONO = "El teléfono debe tener entre 10 y 15 dígitos, solo números y no puede ser todo ceros";

    // Correos: @Email acepta "ana@correo" sin dominio; esto además exige un punto y una terminación (.com, .mx...)
    public static final String PATRON_CORREO = "^[^\\s@]+@[^\\s@]+\\.[^\\s@.]{2,}$";
    public static final String MENSAJE_CORREO = "El correo no tiene un formato válido";

    // Contraseñas nuevas: al menos una mayúscula y un carácter especial (ni letra, ni número, ni espacio)
    public static final String PATRON_PASSWORD = "(?s)^(?=.*\\p{Lu})(?=.*[^\\p{L}\\p{M}\\p{N}\\s]).*$";
    public static final String MENSAJE_PASSWORD = "La contraseña debe tener al menos una mayúscula y un carácter especial";

    // Nombre que UploadService le da a cada archivo: UUID aleatorio + extensión
    public static final String PATRON_ARCHIVO_SUBIDO = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)";

    // Campo opcional: vacío o solo espacios se guarda como null; si no, sin espacios en los extremos
    public static String textoOpcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}

package com.Huellitas.Huellitas.dto;

/*
 * Respuesta estándar para todos los endpoints de la API.
 * Se reutiliza para Usuario, Mascota y SolicitudAdopcion.
 */
public class ApiResponse<T> {

    private boolean exito;
    private String mensaje;
    private T datos;

    public ApiResponse(boolean exito, String mensaje, T datos) {
        this.exito = exito;
        this.mensaje = mensaje;
        this.datos = datos;
    }

    public static <T> ApiResponse<T> ok(String mensaje, T datos) {
        return new ApiResponse<>(true, mensaje, datos);
    }

    public static <T> ApiResponse<T> ok(String mensaje) {
        return new ApiResponse<>(true, mensaje, null);
    }

    public static <T> ApiResponse<T> error(String mensaje) {
        return new ApiResponse<>(false, mensaje, null);
    }

    public boolean isExito() {
        return exito;
    }

    public String getMensaje() {
        return mensaje;
    }

    public T getDatos() {
        return datos;
    }
}

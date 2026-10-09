package com.Huellitas.Huellitas.exception;

import org.springframework.http.HttpStatus;

/*
 * Error de negocio con su código HTTP. El GlobalExceptionHandler lo
 * convierte en ApiResponse.error(mensaje) con ese código.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException badRequest(String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje);
    }

    public static ApiException noAutenticado(String mensaje) {
        return new ApiException(HttpStatus.UNAUTHORIZED, mensaje);
    }

    public static ApiException prohibido(String mensaje) {
        return new ApiException(HttpStatus.FORBIDDEN, mensaje);
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, mensaje);
    }

    public static ApiException conflicto(String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, mensaje);
    }
}

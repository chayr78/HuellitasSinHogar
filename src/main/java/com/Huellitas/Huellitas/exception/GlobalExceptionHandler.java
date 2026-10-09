package com.Huellitas.Huellitas.exception;

import com.Huellitas.Huellitas.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.ObjectError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/*
 * Convierte cualquier error en ApiResponse.error(mensaje) con el código HTTP adecuado.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> manejarApi(ApiException ex) {
        return responder(ex.getStatus(), ex.getMessage());
    }

    // @Valid: se devuelve el primer mensaje de validación
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> manejarValidacion(MethodArgumentNotValidException ex) {
        List<ObjectError> errores = ex.getBindingResult().getAllErrors();
        if (errores.isEmpty()) {
            return responder(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos");
        }
        return responder(HttpStatus.BAD_REQUEST, errores.get(0).getDefaultMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> manejarCuerpoInvalido(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido");
    }

    // Falta un parámetro de la URL, por ejemplo ?tamano=
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> manejarParametroFaltante(MissingServletRequestParameterException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio: " + ex.getParameterName());
    }

    // Falta una parte de un formulario multipart, por ejemplo el campo "archivo"
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> manejarParteFaltante(MissingServletRequestPartException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio: " + ex.getRequestPartName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> manejarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El valor del parámetro '" + ex.getName() + "' no es válido");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> manejarArgumentoInvalido(IllegalArgumentException ex) {
        if (ex.getMessage() == null) {
            return responder(HttpStatus.BAD_REQUEST, "Solicitud no válida");
        }
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> manejarIntegridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT,
                "No se pudo guardar porque entra en conflicto con información existente (por ejemplo, un correo ya registrado)");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> manejarNoEncontrado(NoResourceFoundException ex) {
        return responder(HttpStatus.NOT_FOUND, "El recurso solicitado no existe");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> manejarMetodo(HttpRequestMethodNotSupportedException ex) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido para esta ruta");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> manejarMediaType(HttpMediaTypeNotSupportedException ex) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de contenido no soportado");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> manejarAccesoDenegado(AccessDeniedException ex) {
        return responder(HttpStatus.FORBIDDEN, "No tienes permiso para esta acción");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> manejarArchivoGrande(MaxUploadSizeExceededException ex) {
        return responder(HttpStatus.CONTENT_TOO_LARGE, "La imagen es demasiado grande");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> manejarMultipart(MultipartException ex) {
        return responder(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo enviado");
    }

    // Cualquier otro error: mensaje genérico; el detalle solo en el log
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> manejarGeneral(Exception ex) {
        // Excepciones de Spring MVC que ya traen su propio código HTTP (406, 400...): no son errores del servidor
        if (ex instanceof ErrorResponse errorResponse && !errorResponse.getStatusCode().is5xxServerError()) {
            HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.BAD_REQUEST;
            }
            return responder(status, "Solicitud no válida");
        }
        log.error("Error no controlado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Inténtalo de nuevo más tarde.");
    }

    private static ResponseEntity<ApiResponse<Void>> responder(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(ApiResponse.error(mensaje));
    }
}

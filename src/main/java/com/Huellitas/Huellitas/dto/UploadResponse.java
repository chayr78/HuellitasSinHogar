package com.Huellitas.Huellitas.dto;

/*
 * Respuesta de POST /api/uploads/mascota: URL pública de la imagen.
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record UploadResponse(String url) {}

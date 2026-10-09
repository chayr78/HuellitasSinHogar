package com.Huellitas.Huellitas.controller;

import com.Huellitas.Huellitas.config.UsuarioActual;
import com.Huellitas.Huellitas.dto.ApiResponse;
import com.Huellitas.Huellitas.dto.UploadResponse;
import com.Huellitas.Huellitas.service.UploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/*
 * Endpoints para subir la foto de una mascota o de perfil a Supabase Storage.
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    // POST /api/uploads/mascota  (multipart, campo "archivo")
    @PostMapping(value = "/mascota", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadResponse>> subirFotoMascota(
            @RequestParam("archivo") MultipartFile archivo) {
        String url = uploadService.subirFotoMascota(archivo, UsuarioActual.obtener());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Foto subida correctamente", new UploadResponse(url)));
    }

    // POST /api/uploads/perfil  (multipart, campo "archivo"; cualquier usuario autenticado)
    @PostMapping(value = "/perfil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadResponse>> subirFotoPerfil(
            @RequestParam("archivo") MultipartFile archivo) {
        String url = uploadService.subirFotoPerfil(archivo, UsuarioActual.obtener());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Foto subida correctamente", new UploadResponse(url)));
    }
}

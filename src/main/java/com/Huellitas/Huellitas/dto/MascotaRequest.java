package com.Huellitas.Huellitas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/*
 * Body de POST y PUT /api/mascotas. El publicador sale del token,
 * nunca del body.
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record MascotaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @NotBlank(message = "La especie es obligatoria")
        @Pattern(regexp = "PERRO|GATO", message = "La especie debe ser PERRO o GATO")
        String especie,

        @NotBlank(message = "El sexo es obligatorio")
        @Pattern(regexp = "MACHO|HEMBRA", message = "El sexo debe ser MACHO o HEMBRA")
        String sexo,

        @Size(max = 30, message = "La edad aproximada no puede superar 30 caracteres")
        String edadAproximada,

        @Size(max = 255, message = "La URL de la foto no puede superar 255 caracteres")
        @Pattern(regexp = "^https?://\\S+$", message = "La URL de la foto debe empezar con http:// o https://")
        String fotoUrl,

        @NotBlank(message = "El tamaño es obligatorio")
        @Pattern(regexp = "CHICO|MEDIANO|GRANDE", message = "El tamaño debe ser CHICO, MEDIANO o GRANDE")
        String tamano,

        @NotBlank(message = "El nivel de energía es obligatorio")
        @Pattern(regexp = "BAJA|MEDIA|ALTA", message = "El nivel de energía debe ser BAJA, MEDIA o ALTA")
        String nivelEnergia,

        @Size(max = 5000, message = "La descripción no puede superar 5000 caracteres")
        String descripcion,

        // Opcional. null = no tocar las fotos actuales; lista vacía = quitarlas todas (el service valida)
        List<String> fotosAdicionales
) {}

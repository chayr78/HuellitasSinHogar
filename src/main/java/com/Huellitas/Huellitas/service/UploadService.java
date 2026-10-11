package com.Huellitas.Huellitas.service;

import com.Huellitas.Huellitas.config.UsuarioAutenticado;
import com.Huellitas.Huellitas.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/*
 * Sube fotos de mascotas y de perfil a Supabase Storage y devuelve su URL pública.
 */
@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private static final long TAMANO_MAXIMO = 5L * 1024 * 1024;

    // Tipos de imagen permitidos
    private static final String TIPO_JPEG = "image/jpeg";
    private static final String TIPO_PNG = "image/png";
    private static final String TIPO_WEBP = "image/webp";

    private static final String MENSAJE_SIN_MASCOTA =
            "No detectamos un perro o gato en la foto. Elige otra donde se vea bien a la mascota, con buena luz.";

    private final RestClient restClient = RestClient.create();
    private final DeteccionMascotaService deteccionMascotaService;
    private final String supabaseUrl;
    private final String serviceKey;
    private final String bucket;

    public UploadService(DeteccionMascotaService deteccionMascotaService,
                         @Value("${app.supabase.url}") String supabaseUrl,
                         @Value("${app.supabase.service-key:}") String serviceKey,
                         @Value("${app.supabase.bucket}") String bucket) {
        this.deteccionMascotaService = deteccionMascotaService;
        this.supabaseUrl = supabaseUrl.replaceAll("/+$", "");
        // Sin clave, la subida queda deshabilitada (responde 503)
        if (serviceKey == null) {
            this.serviceKey = "";
        } else {
            this.serviceKey = serviceKey.trim();
        }
        this.bucket = bucket;
    }

    // Fotos de mascotas: carpeta del publicador. Deben tener un perro o gato.
    public String subirFotoMascota(MultipartFile archivo, UsuarioAutenticado publicador) {
        return subirImagen(archivo, String.valueOf(publicador.id()), true);
    }

    // Fotos de perfil: carpeta perfiles/{usuarioId}. Es la foto de una persona: no se busca mascota.
    public String subirFotoPerfil(MultipartFile archivo, UsuarioAutenticado usuario) {
        return subirImagen(archivo, "perfiles/" + usuario.id(), false);
    }

    // Valida la imagen, la sube a la carpeta indicada y devuelve su URL pública
    private String subirImagen(MultipartFile archivo, String carpeta, boolean exigirMascota) {
        if (serviceKey.isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "La subida de fotos no está configurada");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw ApiException.badRequest("Selecciona una imagen");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new ApiException(HttpStatus.CONTENT_TOO_LARGE, "La imagen es demasiado grande");
        }

        byte[] bytes;
        try {
            bytes = archivo.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("No se pudo leer la imagen");
        }

        // El tipo declarado y el contenido real deben coincidir
        String tipo = detectarTipo(bytes);
        if (tipo == null || !tipo.equalsIgnoreCase(archivo.getContentType())) {
            throw ApiException.badRequest("Formato de imagen no válido");
        }

        // Antes de guardar: si no hay perro o gato, el archivo nunca llega a Supabase
        if (exigirMascota && !deteccionMascotaService.contieneMascota(bytes)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, MENSAJE_SIN_MASCOTA);
        }

        // El nombre original se ignora: carpeta + nombre aleatorio
        String ruta = carpeta + "/" + UUID.randomUUID() + "." + extension(tipo);
        subirASupabase(ruta, bytes, tipo);
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + ruta;
    }

    /*
     * Hace un POST a la API de Supabase Storage con la imagen en el cuerpo.
     * RestClient se usa encadenando pasos: URL, cabeceras, cuerpo, enviar y
     * qué hacer si la respuesta es un error.
     */
    private void subirASupabase(String ruta, byte[] bytes, String contentType) {
        try {
            restClient.post()
                    .uri(supabaseUrl + "/storage/v1/object/" + bucket + "/" + ruta)
                    .headers(h -> {
                        h.set("apikey", serviceKey);
                        // Las claves legacy (JWT) también van como Bearer; las sb_secret_ solo como apikey
                        if (serviceKey.startsWith("eyJ")) {
                            h.setBearerAuth(serviceKey);
                        }
                    })
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(bytes)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String cuerpo = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.warn("Supabase Storage respondió {}: {}", response.getStatusCode().value(), cuerpo);
                        throw new ApiException(HttpStatus.BAD_GATEWAY, "No se pudo guardar la imagen");
                    })
                    .toBodilessEntity();
        } catch (ApiException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("No se pudo conectar con Supabase Storage: {}", e.getClass().getSimpleName());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "No se pudo guardar la imagen");
        }
    }

    // Reconoce el tipo por los primeros bytes reales del archivo (su "firma"), no por su nombre
    private static String detectarTipo(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return TIPO_JPEG;
        }
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return TIPO_PNG;
        }
        if (b.length >= 12
                && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return TIPO_WEBP;
        }
        return null;
    }

    private static String extension(String tipo) {
        if (TIPO_JPEG.equals(tipo)) {
            return "jpg";
        }
        if (TIPO_PNG.equals(tipo)) {
            return "png";
        }
        return "webp";
    }
}

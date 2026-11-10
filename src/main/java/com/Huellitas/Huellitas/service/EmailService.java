package com.Huellitas.Huellitas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/*
 * Envío de correos con la API HTTP de Brevo (no SMTP: Railway bloquea SMTP salvo en el plan Pro).
 * Nunca lanza excepciones: si el envío está deshabilitado (app.mail.enabled=false),
 * falta configuración o falla, devuelve false y quien llama decide qué escribir en el log.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String URL_BREVO = "https://api.brevo.com/v3/smtp/email";

    private final boolean habilitado;
    private final String apiKey;
    private final String remitente;
    private final String remitenteNombre;
    private final RestClient http;

    public EmailService(@Value("${app.mail.enabled:false}") boolean habilitado,
                        @Value("${app.mail.brevo-api-key:}") String apiKey,
                        @Value("${app.mail.remitente:}") String remitente,
                        @Value("${app.mail.remitente-nombre:Huellitas sin Hogar}") String remitenteNombre) {
        this.habilitado = habilitado;
        this.apiKey = apiKey;
        this.remitente = remitente;
        this.remitenteNombre = remitenteNombre;

        // Tiempos límite para que un Brevo lento no deje colgada la petición del usuario
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        this.http = RestClient.builder().requestFactory(fabrica).build();
    }

    public boolean enviar(String para, String asunto, String cuerpo) {
        if (!habilitado) {
            return false;
        }
        if (apiKey == null || apiKey.isBlank() || remitente == null || remitente.isBlank()) {
            log.warn("app.mail.enabled=true pero falta BREVO_API_KEY o MAIL_REMITENTE");
            return false;
        }

        Map<String, Object> cuerpoPeticion = Map.of(
                "sender", Map.of("name", remitenteNombre, "email", remitente),
                "to", List.of(Map.of("email", para)),
                "subject", asunto,
                "textContent", cuerpo);
        try {
            http.post()
                    .uri(URL_BREVO)
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpoPeticion)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            // Brevo explica el motivo en el cuerpo (remitente sin verificar, clave inválida...)
            log.warn("Brevo rechazó el correo \"{}\" ({}): {}", asunto, e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (RestClientException e) {
            log.warn("No se pudo enviar el correo \"{}\": {}", asunto, e.getMessage());
            return false;
        }
    }
}

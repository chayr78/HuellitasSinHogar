package com.Huellitas.Huellitas.service;

import com.Huellitas.Huellitas.modelo.Mascota;
import com.Huellitas.Huellitas.modelo.SolicitudAdopcion;
import com.Huellitas.Huellitas.modelo.Usuario;
import com.Huellitas.Huellitas.repository.MascotaRepository;
import com.Huellitas.Huellitas.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/*
 * Envío de correos con la API HTTP de Brevo (no SMTP: Railway lo bloquea).
 *
 * Los métodos públicos son @Async: corren en otro hilo, así que quien los llama (por ejemplo,
 * crear una solicitud) no espera a Brevo. Ninguno lanza excepciones: si el correo está
 * desactivado o Brevo falla, solo se escribe en el log y la aplicación sigue igual.
 * Todo texto escrito por usuarios se escapa con HtmlUtils.htmlEscape antes de ponerlo en el HTML.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String URL_BREVO = "https://api.brevo.com/v3/smtp/email";
    // Endpoint de solo lectura para comprobar la clave al arrancar
    private static final String URL_CUENTA_BREVO = "https://api.brevo.com/v3/account";
    // Las claves SMTP de Brevo empiezan así; la API necesita una API key (xkeysib-...)
    private static final String PREFIJO_CLAVE_SMTP = "xsmtpsib-";

    private final boolean habilitado;
    private final String apiKey;
    private final String remitente;
    private final String nombreRemitente;
    private final String frontendUrl;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RestClient http;
    // Pasa a false si la clave es SMTP o Brevo la rechaza (401) al arrancar: así el sitio no promete correos que no saldrán
    private volatile boolean claveValida = true;

    public EmailService(@Value("${app.mail.enabled:false}") boolean habilitado,
                        @Value("${app.mail.api-key:}") String apiKey,
                        @Value("${app.mail.remitente:}") String remitente,
                        @Value("${app.mail.nombre-remitente:Huellitas Sin Hogar}") String nombreRemitente,
                        @Value("${app.frontend-url}") String frontendUrl,
                        MascotaRepository mascotaRepository,
                        UsuarioRepository usuarioRepository) {
        this.habilitado = habilitado;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.remitente = remitente == null ? "" : remitente.trim();
        this.nombreRemitente = nombreRemitente;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
        this.mascotaRepository = mascotaRepository;
        this.usuarioRepository = usuarioRepository;

        // Tiempos límite para que un Brevo lento no deje ocupado el hilo de correos
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        this.http = RestClient.builder().requestFactory(fabrica).build();

        if (habilitado && this.apiKey.startsWith(PREFIJO_CLAVE_SMTP)) {
            claveValida = false;
            log.error("BREVO_API_KEY es una clave SMTP (xsmtpsib-...). Crea una API key (xkeysib-...) en "
                    + "Brevo > SMTP & API > API Keys. No se enviarán correos.");
        }
    }

    // true si el correo está activado, configurado y con una clave que Brevo no ha rechazado:
    // los avisos se mandarán (en segundo plano)
    public boolean estaActivo() {
        return habilitado && claveValida && !apiKey.isEmpty() && !remitente.isEmpty();
    }

    // Al arrancar, comprueba la clave con Brevo para que un error de configuración se vea en el log
    // desde el inicio y no hasta el primer correo. En segundo plano para no retrasar el arranque.
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void verificarClave() {
        if (!estaActivo()) {
            return;
        }
        try {
            http.get().uri(URL_CUENTA_BREVO).header("api-key", apiKey).retrieve().toBodilessEntity();
            log.info("Brevo: clave válida, los correos están activos");
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 401) {
                claveValida = false;
                // El motivo de Brevo distingue una clave mala de una IP no autorizada (Security > Authorised IPs)
                log.error("Brevo rechazó BREVO_API_KEY (401): {}. No se enviarán correos.", e.getResponseBodyAsString());
            } else {
                log.warn("No se pudo comprobar la clave de Brevo ({}); se intentará al enviar", e.getStatusCode().value());
            }
        } catch (RuntimeException e) {
            // Un fallo de red al arrancar no desactiva el correo: puede ser pasajero
            log.warn("No se pudo comprobar la clave de Brevo: {}", e.getClass().getSimpleName());
        }
    }

    // ---------- Correos de solicitudes ----------

    // Al adoptante, a su correo registrado (no al que escribió en el formulario)
    @Async
    public void enviarConfirmacionAdoptante(SolicitudAdopcion solicitud) {
        try {
            if (!avisarSiDesactivado("confirmación de solicitud")) {
                return;
            }
            Mascota mascota = mascotaRepository.findById(solicitud.getMascotaId()).orElse(null);
            Usuario adoptante = usuarioRepository.findById(solicitud.getAdoptanteId()).orElse(null);
            if (mascota == null || adoptante == null) {
                return;
            }
            Usuario publicador = usuarioRepository.findById(mascota.getPublicadorId()).orElse(null);
            String asunto = "Recibimos tu solicitud para adoptar a " + mascota.getNombre();
            enviar(adoptante.getCorreo(), adoptante.getNombre(), asunto,
                    htmlConfirmacionAdoptante(solicitud, mascota, publicador));
        } catch (RuntimeException e) {
            log.warn("No se pudo preparar la confirmación de la solicitud {}: {}", solicitud.getId(), e.getClass().getSimpleName());
        }
    }

    // Al publicador dueño de la mascota, a su correo registrado
    @Async
    public void enviarSolicitudPublicador(SolicitudAdopcion solicitud) {
        try {
            if (!avisarSiDesactivado("aviso de solicitud al publicador")) {
                return;
            }
            Mascota mascota = mascotaRepository.findById(solicitud.getMascotaId()).orElse(null);
            if (mascota == null) {
                return;
            }
            Usuario publicador = usuarioRepository.findById(mascota.getPublicadorId()).orElse(null);
            if (publicador == null) {
                return;
            }
            String asunto = "Nueva solicitud de adopción para " + mascota.getNombre();
            enviar(publicador.getCorreo(), publicador.getNombre(), asunto,
                    htmlSolicitudPublicador(solicitud, mascota, frontendUrl + "/panel"));
        } catch (RuntimeException e) {
            log.warn("No se pudo preparar el aviso al publicador de la solicitud {}: {}", solicitud.getId(), e.getClass().getSimpleName());
        }
    }

    // Al adoptante, cuando el publicador marca su solicitud como CONTACTADA o CERRADA
    @Async
    public void enviarCambioEstadoAdoptante(SolicitudAdopcion solicitud) {
        try {
            if (!avisarSiDesactivado("cambio de estado de solicitud")) {
                return;
            }
            Mascota mascota = mascotaRepository.findById(solicitud.getMascotaId()).orElse(null);
            Usuario adoptante = usuarioRepository.findById(solicitud.getAdoptanteId()).orElse(null);
            if (mascota == null || adoptante == null) {
                return;
            }
            String asunto = "Tu solicitud para adoptar a " + mascota.getNombre() + " cambió de estado";
            enviar(adoptante.getCorreo(), adoptante.getNombre(), asunto,
                    htmlCambioEstado(solicitud, mascota, frontendUrl + "/mis-solicitudes"));
        } catch (RuntimeException e) {
            log.warn("No se pudo preparar el aviso de cambio de estado de la solicitud {}: {}", solicitud.getId(), e.getClass().getSimpleName());
        }
    }

    // ---------- Contraseña ----------

    // El enlace lleva el token: en producción nunca se escribe en el log. Solo en desarrollo
    // (frontend en localhost) y sin correo, se anota para poder restablecer contraseñas sin Brevo.
    @Async
    public void enviarRestablecimiento(String correo, String nombre, String enlace, int minutosVigencia) {
        try {
            if (!avisarSiDesactivado("restablecimiento de contraseña")) {
                if (esDesarrollo()) {
                    log.info("[DEV] Enlace de restablecimiento: {}", enlace);
                }
                return;
            }
            String html = "<p>Hola " + escapar(nombre) + ",</p>"
                    + "<p>Recibimos una solicitud para restablecer tu contraseña en Huellitas Sin Hogar.</p>"
                    + "<p><a href=\"" + escapar(enlace) + "\">Crear una contraseña nueva</a> "
                    + "(el enlace vale " + minutosVigencia + " minutos).</p>"
                    + "<p>Si no fuiste tú, ignora este correo.</p>";
            enviar(correo, nombre, "Restablece tu contraseña", plantilla(html));
        } catch (RuntimeException e) {
            log.warn("No se pudo preparar el correo de restablecimiento: {}", e.getClass().getSimpleName());
        }
    }

    // ---------- Envío a Brevo ----------

    // Hace el POST a Brevo. Nunca lanza excepciones; en el log no van la clave ni el contenido.
    private void enviar(String destinatario, String nombre, String asunto, String html) {
        Map<String, Object> cuerpo = Map.of(
                "sender", Map.of("name", nombreRemitente, "email", remitente),
                "to", List.of(Map.of("email", destinatario, "name", nombre == null ? destinatario : nombre)),
                "subject", asunto,
                "htmlContent", html);
        try {
            http.post()
                    .uri(URL_BREVO)
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Correo enviado: \"{}\"", asunto);
        } catch (RestClientResponseException e) {
            // Un rechazo al enviar (incluido un 401 pasajero) solo se anota: no desactiva el correo.
            // La clave mal configurada ya la detecta verificarClave() al arrancar.
            // Brevo explica el motivo en el cuerpo (remitente sin verificar, clave inválida...)
            log.warn("Brevo rechazó el correo \"{}\" ({}): {}", asunto, e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el correo \"{}\": {}", asunto, e.getClass().getSimpleName());
        }
    }

    // Devuelve true si se puede enviar; si no, lo anota en el log (sin datos sensibles)
    private boolean avisarSiDesactivado(String tipo) {
        if (estaActivo()) {
            return true;
        }
        if (habilitado && !claveValida) {
            log.warn("Correo desactivado: Brevo no acepta BREVO_API_KEY (no se envió: {})", tipo);
        } else if (habilitado) {
            log.warn("Correo desactivado: falta BREVO_API_KEY o MAIL_REMITENTE (no se envió: {})", tipo);
        } else {
            log.info("Correo desactivado (no se envió: {})", tipo);
        }
        return false;
    }

    // Desarrollo local: el frontend corre en localhost
    private boolean esDesarrollo() {
        return frontendUrl.startsWith("http://localhost") || frontendUrl.startsWith("http://127.0.0.1");
    }

    // ---------- HTML de cada correo (package-private para las pruebas) ----------

    static String htmlConfirmacionAdoptante(SolicitudAdopcion s, Mascota mascota, Usuario publicador) {
        String nombrePublicador = publicador == null ? "Quien la publicó" : publicador.getNombre();
        String html = "<h2 style=\"color:#2f6f4f\">¡Recibimos tu solicitud!</h2>"
                + fotoMascota(mascota)
                + "<h3>" + escapar(mascota.getNombre()) + "</h3>"
                + "<p>" + fila("Especie", etiqueta(mascota.getEspecie()))
                + fila("Tamaño", etiqueta(mascota.getTamano()))
                + fila("Edad", textoOVacio(mascota.getEdadAproximada()))
                + fila("Publicada por", nombrePublicador) + "</p>"
                + "<h3>Los datos que enviaste</h3>"
                + "<p>" + fila("Nombre", s.getNombreContacto())
                + fila("Teléfono", s.getTelefonoContacto())
                + fila("Mensaje", textoOVacio(s.getMensaje()))
                + fila("Compatibilidad", compatibilidad(s.getPorcentajeCompatibilidad())) + "</p>"
                + "<p><strong>El publicador se pondrá en contacto contigo.</strong></p>";
        return plantilla(html);
    }

    static String htmlSolicitudPublicador(SolicitudAdopcion s, Mascota mascota, String enlacePanel) {
        String html = "<h2 style=\"color:#2f6f4f\">Nueva solicitud para " + escapar(mascota.getNombre()) + "</h2>"
                + fotoMascota(mascota)
                + "<h3>Datos de quien la envió</h3>"
                + "<p>" + fila("Nombre", s.getNombreContacto())
                + fila("Teléfono", s.getTelefonoContacto())
                + fila("Correo", s.getCorreoContacto())
                + fila("Mensaje", textoOVacio(s.getMensaje()))
                + fila("Compatibilidad", compatibilidad(s.getPorcentajeCompatibilidad()))
                + fila("Experiencia previa con mascotas", siNo(s.getExperienciaPrevia()))
                + fila("Niños pequeños en casa", siNo(s.getNinosPequenos()))
                + fila("Otras mascotas en casa", siNo(s.getOtrasMascotas())) + "</p>"
                + "<p><a href=\"" + escapar(enlacePanel) + "\" style=\"background:#2f6f4f;color:#fff;"
                + "padding:10px 18px;border-radius:6px;text-decoration:none\">Revisar en mi panel</a></p>";
        return plantilla(html);
    }

    static String htmlCambioEstado(SolicitudAdopcion s, Mascota mascota, String enlaceSolicitudes) {
        String texto = "CONTACTADA".equals(s.getEstado())
                ? "Tu solicitud fue marcada como <strong>contactada</strong>: pronto te escribirán, si no lo han hecho ya."
                : "Tu solicitud fue marcada como <strong>cerrada</strong>. Gracias por querer darle un hogar; "
                + "hay más mascotas esperando una familia.";
        String html = "<h2 style=\"color:#2f6f4f\">Tu solicitud para " + escapar(mascota.getNombre()) + "</h2>"
                + fotoMascota(mascota)
                + "<p>" + texto + "</p>"
                + "<p><a href=\"" + escapar(enlaceSolicitudes) + "\">Ver mis solicitudes</a></p>";
        return plantilla(html);
    }

    // ---------- Utilidades de HTML ----------

    private static String plantilla(String contenido) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:auto;color:#333\">"
                + contenido
                + "<hr><p style=\"font-size:12px;color:#888\">Huellitas Sin Hogar</p></div>";
    }

    // La foto principal (URL pública de Supabase); si la mascota no tiene, no se pone nada
    private static String fotoMascota(Mascota mascota) {
        String foto = Textos.textoOpcional(mascota.getFotoUrl());
        if (foto == null) {
            return "";
        }
        return "<p><img src=\"" + escapar(foto) + "\" alt=\"" + escapar(mascota.getNombre())
                + "\" style=\"max-width:400px;width:100%;border-radius:8px\"></p>";
    }

    // "Etiqueta: valor" en una línea, con el valor escapado
    private static String fila(String etiqueta, String valor) {
        return "<strong>" + etiqueta + ":</strong> " + escapar(valor) + "<br>";
    }

    private static String escapar(String texto) {
        return texto == null ? "" : HtmlUtils.htmlEscape(texto);
    }

    private static String textoOVacio(String texto) {
        String limpio = Textos.textoOpcional(texto);
        return limpio == null ? "Sin especificar" : limpio;
    }

    // "MEDIANO" -> "Mediano"
    private static String etiqueta(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "Sin especificar";
        }
        return valor.charAt(0) + valor.substring(1).toLowerCase();
    }

    private static String compatibilidad(Short porcentaje) {
        return porcentaje == null ? "No respondió la encuesta" : porcentaje + " %";
    }

    private static String siNo(Boolean valor) {
        if (valor == null) {
            return "Sin respuesta";
        }
        return valor ? "Sí" : "No";
    }
}

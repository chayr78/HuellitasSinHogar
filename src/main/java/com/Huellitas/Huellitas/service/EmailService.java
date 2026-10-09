package com.Huellitas.Huellitas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/*
 * Envío de correos. Nunca lanza excepciones: si el envío está deshabilitado
 * (app.mail.enabled=false) o falla, devuelve false y quien llama decide
 * qué escribir en el log.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final boolean habilitado;
    // Solo existe si se configuró spring.mail.host
    private final ObjectProvider<JavaMailSender> mailSender;

    public EmailService(@Value("${app.mail.enabled:false}") boolean habilitado,
                        ObjectProvider<JavaMailSender> mailSender) {
        this.habilitado = habilitado;
        this.mailSender = mailSender;
    }

    public boolean enviar(String para, String asunto, String cuerpo) {
        if (!habilitado) {
            return false;
        }

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("app.mail.enabled=true pero no hay servidor de correo configurado (spring.mail.host)");
            return false;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(para);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            sender.send(mensaje);
            return true;
        } catch (MailException e) {
            log.warn("No se pudo enviar el correo \"{}\": {}", asunto, e.getMessage());
            return false;
        }
    }
}

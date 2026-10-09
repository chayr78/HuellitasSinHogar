package com.Huellitas.Huellitas.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/*
 * Clave y codificadores de los tokens JWT (HS256).
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
    private static final int BYTES_MINIMOS = 32;

    @Bean
    public SecretKey jwtSecretKey(@Value("${app.jwt.secret:}") String secreto) {
        if (secreto == null || secreto.isBlank()) {
            byte[] aleatoria = new byte[BYTES_MINIMOS];
            new SecureRandom().nextBytes(aleatoria);
            log.warn("app.jwt.secret (JWT_SECRET) está vacío: se generó una clave aleatoria. "
                    + "Los tokens dejarán de ser válidos cada vez que se reinicie el servidor.");
            return new SecretKeySpec(aleatoria, "HmacSHA256");
        }

        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < BYTES_MINIMOS) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) debe tener al menos "
                    + BYTES_MINIMOS + " bytes para HS256; tiene " + bytes.length + ".");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}

package com.Huellitas.Huellitas.service;

import com.Huellitas.Huellitas.modelo.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/*
 * Emite los tokens JWT (HS256): subject = id del usuario, claims correo y rol.
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Duration duracion;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${app.jwt.expiration-minutes}") long minutos) {
        this.jwtEncoder = jwtEncoder;
        this.duracion = Duration.ofMinutes(minutos);
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("correo", usuario.getCorreo())
                .claim("rol", usuario.getRol())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(duracion))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}

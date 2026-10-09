package com.Huellitas.Huellitas.config;

import com.Huellitas.Huellitas.modelo.Constantes;
import com.Huellitas.Huellitas.modelo.Usuario;
import com.Huellitas.Huellitas.repository.UsuarioRepository;
import com.Huellitas.Huellitas.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/*
 * Crea el primer ADMINISTRADOR al arrancar si app.admin.correo y
 * app.admin.password tienen valor y todavía no existe ninguno.
 */
@Component
public class AdminInicializador implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicializador.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String correo;
    private final String password;

    public AdminInicializador(UsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder,
                              @Value("${app.admin.correo:}") String correo,
                              @Value("${app.admin.password:}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (correo.isBlank() || password.isBlank()) {
            return;
        }
        if (usuarioRepository.existsByRol(Constantes.ROL_ADMINISTRADOR)) {
            return;
        }

        // BCrypt rechaza contraseñas de más de 72 bytes; sin esto el servidor no arrancaría
        if (AuthService.passwordDemasiadoLarga(password)) {
            log.warn("No se creó el administrador inicial: app.admin.password supera los 72 bytes");
            return;
        }

        String correoNormalizado = AuthService.normalizarCorreo(correo);
        if (usuarioRepository.existsByCorreo(correoNormalizado)) {
            log.warn("No se creó el administrador inicial: el correo {} ya pertenece a otra cuenta", correoNormalizado);
            return;
        }

        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setCorreo(correoNormalizado);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRol(Constantes.ROL_ADMINISTRADOR);
        admin.setActivo(true);
        admin.setFechaRegistro(LocalDateTime.now());
        usuarioRepository.save(admin);

        log.info("Administrador inicial creado con el correo {}", correoNormalizado);
    }
}

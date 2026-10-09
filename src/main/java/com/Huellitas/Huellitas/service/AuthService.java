package com.Huellitas.Huellitas.service;

import com.Huellitas.Huellitas.config.UsuarioActual;
import com.Huellitas.Huellitas.dto.ActualizarPerfilRequest;
import com.Huellitas.Huellitas.dto.AuthResponse;
import com.Huellitas.Huellitas.dto.LoginRequest;
import com.Huellitas.Huellitas.dto.OlvidePasswordRequest;
import com.Huellitas.Huellitas.dto.RegistroRequest;
import com.Huellitas.Huellitas.dto.RestablecerPasswordRequest;
import com.Huellitas.Huellitas.dto.UsuarioResponse;
import com.Huellitas.Huellitas.exception.ApiException;
import com.Huellitas.Huellitas.modelo.Constantes;
import com.Huellitas.Huellitas.modelo.Usuario;
import com.Huellitas.Huellitas.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/*
 * Registro, inicio de sesión y restablecimiento de contraseña.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final int MINUTOS_TOKEN_RESET = 30;
    // BCrypt solo usa los primeros 72 bytes; Spring Security rechaza contraseñas más largas
    private static final int BYTES_MAX_PASSWORD = 72;
    private static final String MENSAJE_LOGIN_INCORRECTO = "Correo o contraseña incorrectos";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final String frontendUrl;
    // Prefijo que deben tener las fotos de perfil (Supabase Storage público)
    private final String prefijoFotos;
    private final SecureRandom random = new SecureRandom();
    // Hash contra el que se compara cuando el correo no existe, para que el login tarde lo mismo
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EmailService emailService,
                       @Value("${app.frontend-url}") String frontendUrl,
                       @Value("${app.supabase.url}") String supabaseUrl) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
        this.prefijoFotos = supabaseUrl.replaceAll("/+$", "") + "/storage/v1/object/public/";
        this.hashFicticio = passwordEncoder.encode("contraseña-ficticia-no-usable");
    }

    // El rol ya llega validado (PUBLICADOR o ADOPTANTE) por @Pattern en RegistroRequest
    @Transactional
    public AuthResponse registrar(RegistroRequest req) {
        String correo = normalizarCorreo(req.correo());
        if (usuarioRepository.existsByCorreo(correo)) {
            throw ApiException.conflicto("Ya existe una cuenta con ese correo");
        }
        boolean esPublicador = Constantes.ROL_PUBLICADOR.equals(req.rol());
        if (esPublicador && req.tipoPublicador() == null) {
            throw ApiException.badRequest("Indica si publicas como REFUGIO o PARTICULAR");
        }
        validarLongitudPassword(req.password());

        Usuario usuario = new Usuario();
        usuario.setNombre(req.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(req.password()));
        usuario.setTelefono(Textos.textoOpcional(req.telefono()));
        usuario.setRol(req.rol());
        if (esPublicador) {
            usuario.setTipoPublicador(req.tipoPublicador());
        }
        usuario.setUbicacion(Textos.textoOpcional(req.ubicacion()));
        usuario.setActivo(true);
        usuario.setFechaRegistro(LocalDateTime.now());

        Usuario guardado = usuarioRepository.save(usuario);
        return new AuthResponse(jwtService.generarToken(guardado), UsuarioResponse.desde(guardado));
    }

    public AuthResponse login(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByCorreo(normalizarCorreo(req.correo())).orElse(null);

        if (usuario == null) {
            // Se compara igual para que la respuesta tarde lo mismo exista o no el correo
            passwordEncoder.matches(req.password(), hashFicticio);
            throw ApiException.noAutenticado(MENSAJE_LOGIN_INCORRECTO);
        }
        if (!passwordCoincide(req.password(), usuario.getPasswordHash())) {
            throw ApiException.noAutenticado(MENSAJE_LOGIN_INCORRECTO);
        }
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw ApiException.prohibido("Tu cuenta está desactivada. Contacta al administrador.");
        }

        return new AuthResponse(jwtService.generarToken(usuario), UsuarioResponse.desde(usuario));
    }

    public UsuarioResponse me() {
        Long id = UsuarioActual.obtener().id();
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            throw ApiException.noEncontrado("Usuario no encontrado");
        }
        return UsuarioResponse.desde(usuario);
    }

    // PUT /api/auth/me: solo cambia nombre, teléfono, ubicación y foto
    @Transactional
    public UsuarioResponse actualizarPerfil(ActualizarPerfilRequest req) {
        Long id = UsuarioActual.obtener().id();
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            throw ApiException.noEncontrado("Usuario no encontrado");
        }

        String foto = Textos.textoOpcional(req.fotoPerfilUrl());
        if (foto != null && !foto.startsWith(prefijoFotos)) {
            throw ApiException.badRequest("La foto de perfil debe subirse desde la aplicación");
        }

        usuario.setNombre(req.nombre().trim());
        usuario.setTelefono(Textos.textoOpcional(req.telefono()));
        usuario.setUbicacion(Textos.textoOpcional(req.ubicacion()));
        usuario.setFotoPerfilUrl(foto);
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    /*
     * Siempre termina sin error, exista o no el correo, para no revelar
     * qué correos están registrados.
     */
    @Transactional
    public void olvidePassword(OlvidePasswordRequest req) {
        Usuario usuario = usuarioRepository.findByCorreo(normalizarCorreo(req.correo())).orElse(null);
        if (usuario == null) {
            return;
        }

        // Al usuario se le envía el token; en la base solo se guarda su hash
        String token = generarTokenAleatorio();
        usuario.setResetTokenHash(sha256(token));
        usuario.setResetTokenExpira(LocalDateTime.now().plusMinutes(MINUTOS_TOKEN_RESET));
        usuarioRepository.save(usuario);

        String enlace = frontendUrl + "/restablecer?token=" + token;
        String cuerpo = "Hola " + usuario.getNombre() + ",\n\n"
                + "Recibimos una solicitud para restablecer tu contraseña en Huellitas sin Hogar.\n"
                + "Abre este enlace (válido por " + MINUTOS_TOKEN_RESET + " minutos):\n\n"
                + enlace + "\n\n"
                + "Si no fuiste tú, ignora este correo.";

        boolean enviado = emailService.enviar(usuario.getCorreo(), "Restablece tu contraseña", cuerpo);
        if (!enviado) {
            log.info("[DEV] Enlace de restablecimiento: {}", enlace);
        }
    }

    @Transactional
    public void restablecerPassword(RestablecerPasswordRequest req) {
        Usuario usuario = usuarioRepository.findByResetTokenHash(sha256(req.token())).orElse(null);
        if (usuario == null || !tokenResetVigente(usuario)) {
            throw ApiException.badRequest("El enlace no es válido o ya expiró. Solicita uno nuevo.");
        }

        validarLongitudPassword(req.nuevaPassword());
        usuario.setPasswordHash(passwordEncoder.encode(req.nuevaPassword()));
        // El enlace solo se puede usar una vez
        usuario.setResetTokenHash(null);
        usuario.setResetTokenExpira(null);
        usuarioRepository.save(usuario);
    }

    // ---------- Utilidades públicas (también las usa AdminInicializador) ----------

    public static String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean passwordDemasiadoLarga(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > BYTES_MAX_PASSWORD;
    }

    // ---------- Utilidades privadas ----------

    private boolean passwordCoincide(String password, String hash) {
        try {
            return passwordEncoder.matches(password, hash);
        } catch (IllegalArgumentException e) {
            // Contraseña de más de 72 bytes: no puede coincidir con ningún hash
            return false;
        }
    }

    private static void validarLongitudPassword(String password) {
        if (passwordDemasiadoLarga(password)) {
            throw ApiException.badRequest("La contraseña es demasiado larga; usa menos caracteres especiales o acentos");
        }
    }

    private static boolean tokenResetVigente(Usuario usuario) {
        LocalDateTime expira = usuario.getResetTokenExpira();
        return expira != null && expira.isAfter(LocalDateTime.now());
    }

    private String generarTokenAleatorio() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}

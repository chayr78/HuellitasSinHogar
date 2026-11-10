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
    private final IntentosLogin intentosLogin;
    private final String frontendUrl;
    // Carpeta pública de las fotos de perfil en Supabase Storage; cada usuario tiene la suya: {prefijo}{id}/
    private final String prefijoFotosPerfil;
    private final SecureRandom random = new SecureRandom();
    // Hash contra el que se compara cuando el correo no existe, para que el login tarde lo mismo
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EmailService emailService,
                       IntentosLogin intentosLogin,
                       @Value("${app.frontend-url}") String frontendUrl,
                       @Value("${app.supabase.url}") String supabaseUrl,
                       @Value("${app.supabase.bucket}") String bucket) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.intentosLogin = intentosLogin;
        this.frontendUrl = frontendUrl;
        this.prefijoFotosPerfil = supabaseUrl.replaceAll("/+$", "") + "/storage/v1/object/public/" + bucket + "/perfiles/";
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
        if (esPublicador && Textos.textoOpcional(req.ubicacion()) == null) {
            throw ApiException.badRequest("Indica el municipio donde se encuentran las mascotas");
        }
        validarNombre(req.nombre(), esPublicador ? req.tipoPublicador() : null);
        validarLongitudPassword(req.password());

        Usuario usuario = new Usuario();
        usuario.setNombre(req.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(req.password()));
        usuario.setTelefono(Textos.soloDigitos(req.telefono()));
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
        String correo = normalizarCorreo(req.correo());
        intentosLogin.verificar(correo);
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);

        if (usuario == null) {
            // Se compara igual para que la respuesta tarde lo mismo exista o no el correo
            passwordEncoder.matches(req.password(), hashFicticio);
            intentosLogin.registrarFallo(correo);
            throw ApiException.noAutenticado(MENSAJE_LOGIN_INCORRECTO);
        }
        if (!passwordCoincide(req.password(), usuario.getPasswordHash())) {
            intentosLogin.registrarFallo(correo);
            throw ApiException.noAutenticado(MENSAJE_LOGIN_INCORRECTO);
        }
        intentosLogin.limpiar(correo);
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

        // Solo una foto que este usuario subió a su carpeta (o la que ya tenía), no la de otra persona o mascota
        String foto = Textos.textoOpcional(req.fotoPerfilUrl());
        boolean esFotoActual = foto != null && foto.equals(usuario.getFotoPerfilUrl());
        String prefijo = prefijoFotosPerfil + id + "/";
        if (foto != null && !esFotoActual
                && (!foto.startsWith(prefijo) || !foto.substring(prefijo.length()).matches(Textos.PATRON_ARCHIVO_SUBIDO))) {
            throw ApiException.badRequest("La foto de perfil debe subirse desde la aplicación");
        }
        validarNombre(req.nombre(), usuario.getTipoPublicador());

        usuario.setNombre(req.nombre().trim());
        usuario.setTelefono(Textos.soloDigitos(req.telefono()));
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
        // Con la contraseña nueva ya puede volver a intentar aunque estuviera bloqueado
        intentosLogin.limpiar(usuario.getCorreo());
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

    // Solo letras para personas; los refugios también pueden usar números
    private static void validarNombre(String nombre, String tipoPublicador) {
        boolean esRefugio = Constantes.TIPO_REFUGIO.equals(tipoPublicador);
        String patron = esRefugio ? Textos.PATRON_NOMBRE_REFUGIO : Textos.PATRON_NOMBRE;
        if (!nombre.trim().matches(patron)) {
            throw ApiException.badRequest(esRefugio ? Textos.MENSAJE_NOMBRE_REFUGIO : Textos.MENSAJE_NOMBRE);
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

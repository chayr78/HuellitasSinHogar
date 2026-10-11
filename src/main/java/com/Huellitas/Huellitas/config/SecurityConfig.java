package com.Huellitas.Huellitas.config;

import com.Huellitas.Huellitas.modelo.Constantes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/*
 * Seguridad de la API: qué rutas son públicas, qué rol pide cada una,
 * cómo se lee el token JWT y cómo se responden los errores 401 y 403.
 */
@Configuration
public class SecurityConfig {

    private static final String PUBLICADOR = Constantes.ROL_PUBLICADOR;
    private static final String ADOPTANTE = Constantes.ROL_ADOPTANTE;
    private static final String ADMINISTRADOR = Constantes.ROL_ADMINISTRADOR;

    private static final RequestMatcher MASCOTAS_MIAS = ruta(HttpMethod.GET, "/api/mascotas/mias");

    // Rutas que no necesitan iniciar sesión
    private static final RequestMatcher RUTAS_PUBLICAS = new OrRequestMatcher(
            ruta(HttpMethod.POST, "/api/auth/**"),
            ruta(HttpMethod.GET, "/api/mascotas/disponibles"),
            ruta(HttpMethod.GET, "/api/mascotas/adoptados"),
            ruta(HttpMethod.GET, "/api/mascotas/{id}"),
            ruta(HttpMethod.POST, "/api/solicitudes/compatibilidad"));

    // 401: no hay token o no es válido
    private static final AuthenticationEntryPoint RESPUESTA_401 = (request, response, e) ->
            escribirError(response, HttpServletResponse.SC_UNAUTHORIZED, "Debes iniciar sesión");

    // 403: hay sesión, pero el rol no alcanza
    private static final AccessDeniedHandler RESPUESTA_403 = (request, response, e) ->
            escribirError(response, HttpServletResponse.SC_FORBIDDEN, "No tienes permiso para esta acción");

    /*
     * Las reglas se revisan de arriba hacia abajo y gana la primera que encaja,
     * por eso el orden importa.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtUsuarioConverter jwtUsuarioConverter) throws Exception {
        http
                // API sin sesiones ni cookies: cada petición trae su token, así que CSRF no aplica
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. Preflight de CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Página de error interna de Spring: sin esto un error se convertiría en 401
                        .requestMatchers("/error").permitAll()

                        // 2. Antes que /api/mascotas/{id}, que también encajaría con "mias"
                        .requestMatchers(MASCOTAS_MIAS).hasRole(PUBLICADOR)

                        // 3. Autenticación
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/auth/me").authenticated()
                        // Antes que /api/uploads/**, que exige PUBLICADOR
                        .requestMatchers(HttpMethod.POST, "/api/uploads/perfil").authenticated()

                        // 4. Públicas (incluye POST /api/auth/**)
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()

                        // 5. Publicador (PUT/PATCH/DELETE también el administrador; el service verifica que sea el dueño)
                        .requestMatchers(HttpMethod.POST, "/api/mascotas", "/api/mascotas/**").hasRole(PUBLICADOR)
                        .requestMatchers(HttpMethod.PUT, "/api/mascotas/**").hasAnyRole(PUBLICADOR, ADMINISTRADOR)
                        .requestMatchers(HttpMethod.PATCH, "/api/mascotas/**").hasAnyRole(PUBLICADOR, ADMINISTRADOR)
                        .requestMatchers(HttpMethod.DELETE, "/api/mascotas/**").hasAnyRole(PUBLICADOR, ADMINISTRADOR)
                        .requestMatchers(HttpMethod.POST, "/api/uploads/**").hasRole(PUBLICADOR)

                        // 6. Adoptante
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes").hasRole(ADOPTANTE)
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes/mias", "/api/solicitudes/activa").hasRole(ADOPTANTE)

                        // 7. Administrador
                        .requestMatchers("/api/admin/**").hasRole(ADMINISTRADOR)

                        // 8. Cualquier usuario autenticado (la propiedad se verifica en el service)
                        .requestMatchers("/api/notificaciones/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes/mascota/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/solicitudes/**").authenticated()

                        // 9. Todo lo demás
                        .anyRequest().authenticated()
                )
                // Lectura y validación del token JWT de la cabecera Authorization
                .oauth2ResourceServer(oauth -> oauth
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtUsuarioConverter))
                        .authenticationEntryPoint(RESPUESTA_401)
                        .accessDeniedHandler(RESPUESTA_403)
                )
                // Los mismos 401/403 para peticiones sin token
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(RESPUESTA_401)
                        .accessDeniedHandler(RESPUESTA_403)
                );

        return http.build();
    }

    private static RequestMatcher ruta(HttpMethod metodo, String patron) {
        return PathPatternRequestMatcher.withDefaults().matcher(metodo, patron);
    }

    /*
     * En las rutas públicas se ignora el token: uno caducado, firmado con una clave
     * anterior o de una cuenta desactivada no debe bloquear el catálogo ni el login con 401.
     */
    private static BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver porDefecto = new DefaultBearerTokenResolver();
        return (HttpServletRequest request) -> {
            if (RUTAS_PUBLICAS.matches(request) && !MASCOTAS_MIAS.matches(request)) {
                return null;
            }
            return porDefecto.resolve(request);
        };
    }

    // Mismo formato que ApiResponse.error(). Los mensajes son constantes, sin caracteres que escapar.
    private static void escribirError(HttpServletResponse response, int status, String mensaje) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"exito\":false,\"mensaje\":\"" + mensaje + "\",\"datos\":null}");
    }

    // Orígenes (dominios del frontend) que pueden llamar a la API desde el navegador
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String origenes) {

        List<String> origenesPermitidos = new ArrayList<>();
        for (String origen : origenes.split(",")) {
            if (!origen.trim().isEmpty()) {
                origenesPermitidos.add(origen.trim());
            }
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origenesPermitidos);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

package com.photobogota.api.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final RateLimitFilter rateLimitFilter;
        private final MantenimientoFilter mantenimientoFilter;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                        RateLimitFilter rateLimitFilter,
                        MantenimientoFilter mantenimientoFilter) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.rateLimitFilter = rateLimitFilter;
                this.mantenimientoFilter = mantenimientoFilter;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                http
                                .cors(Customizer.withDefaults())
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // PREFILGHT CORS
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                                                // RUTAS DE DOCUMENTACIÓN (Swagger)
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**",
                                                                "/v3/api-docs")
                                                .permitAll()
                                                // RUTAS PÚBLICAS
                                                // Autenticación
                                                .requestMatchers(
                                                                "/api/v1/auth/login",
                                                                "/api/v1/auth/register",
                                                                "/api/v1/auth/passwords/recovery-request",
                                                                "/api/v1/auth/passwords/reset",
                                                                "/api/v1/auth/refresh")
                                                .permitAll()

                                                // Aspirantes: crear solicitud sin cuenta y subir su documento
                                                .requestMatchers(HttpMethod.POST, "/api/v1/aspirantes").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/imagenes/aspirante-documento")
                                                .permitAll()
                                                // Aspirantes: consultar el estado de la propia solicitud por código
                                                // y reenviar documentos tras una corrección (sin cuenta)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/aspirantes/codigo/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.PUT, "/api/v1/aspirantes/codigo/*/reenviar")
                                                .permitAll()
                                                // El resto de /api/v1/aspirantes/** (listar, ver por id/email,
                                                // aprobar, rechazar, corregir, comentarios, estadísticas) requiere
                                                // MOD o ADMIN; queda cubierto por @PreAuthorize en el controlador
                                                // y por la regla genérica de autenticación más abajo.

                                                // Spots públicos (solo lectura)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/spots/**").permitAll()

                                                // Promociones públicas (solo lectura): el mapa consulta las activas
                                                // y las de cada local. Crear/gestionar requiere rol SOCIO (vía @PreAuthorize).
                                                .requestMatchers(HttpMethod.GET, "/api/v1/promociones/**").permitAll()

                                                // Categorías y localidades públicas
                                                .requestMatchers(HttpMethod.GET, "/api/v1/categorias",
                                                                "/api/v1/localidades")
                                                .permitAll()

// Perfiles públicos (solo lectura)
                                                 .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/perfil/**")
                                                 .permitAll()
                                                 // Rutas del propio usuario (autenticación requerida) - más específicas primero
                                                 .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/me/spots")
                                                 .authenticated()
                                                 .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/me/resenas")
                                                 .authenticated()
                                                 .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/me/puntos")
                                                 .authenticated()
                                                 .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/me/guardados")
                                                 .authenticated()
                                                 .requestMatchers(HttpMethod.POST, "/api/v1/usuarios/me/guardados/**")
                                                 .authenticated()
                                                 .requestMatchers(HttpMethod.DELETE, "/api/v1/usuarios/me/guardados/**")
                                                 .authenticated()
                                                 // Spots y reseñas públicos de cualquier usuario
                                                 .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/*/spots")
                                                 .permitAll()
                                                 .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/*/resenas")
                                                 .permitAll()

                                                // Monitoreo / Actuator
                                                .requestMatchers("/actuator/**", "/api/v1/actuator/**").permitAll()

                                                // Estado de mantenimiento (lo consulta el front para mostrar el aviso,
                                                // incluso con el resto de rutas bloqueadas por el MantenimientoFilter)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/mantenimiento/estado")
                                                .permitAll()

                                                // TODAS LAS RUTAS DE ADMIN (forma compacta)
                                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                                                // RUTAS PROTEGIDAS (autenticación requerida)
                                                .requestMatchers(HttpMethod.POST, "/api/v1/spots").authenticated()

                                                // Crear reseñas
                                                .requestMatchers(HttpMethod.POST, "/api/v1/spots/*/resenas")
                                                .authenticated()

                                                // Gestión del propio usuario
                                                .requestMatchers(
                                                                "/api/v1/usuarios/perfil",
                                                                "/api/v1/usuarios/me/password",
                                                                "/api/v1/auth/me")
                                                .authenticated()

                                                // Autoeliminación de cuenta: solo MIEMBRO
                                                .requestMatchers("/api/v1/usuarios/me/eliminacion/**")
                                                .hasRole("MIEMBRO")

                                                // RUTAS DE MODERADOR
                                                .requestMatchers("/api/v1/moderador/**").hasRole("MOD")
                                                // RUTAS DE ADMINISTRADOR
                                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                                                // LO DEMÁS bajo /api/v1
                                                // Cualquier otra ruta dentro de /api/v1 requiere autenticación
                                                .requestMatchers("/api/v1/**").authenticated()
                                                .anyRequest().authenticated())

                                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(jwtAuthenticationFilter, RateLimitFilter.class)
                                .addFilterAfter(mantenimientoFilter, RateLimitFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOriginPatterns(Arrays.asList(
                                "http://localhost:5173",
                                "http://127.0.0.1:5173",
                                "http://192.168.*.*:5173",
                                "http://localhost:3000",
                                "http://127.0.0.1:3000",
                                "http://localhost:55352"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(
                                Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With"));
                configuration.setAllowCredentials(true);
                configuration.setExposedHeaders(Arrays.asList("Authorization")); // Exponer el header Authorization

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
                        throws Exception {
                return config.getAuthenticationManager();
        }
}
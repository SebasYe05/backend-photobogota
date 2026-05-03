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

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                        RateLimitFilter rateLimitFilter) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.rateLimitFilter = rateLimitFilter;
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

                                                // RUTAS PÚBLICAS
                                                // Autenticación
                                                .requestMatchers(
                                                                "/api/v1/auth/login",
                                                                "/api/v1/auth/register",
                                                                "/api/v1/auth/passwords/recovery-request",
                                                                "/api/v1/auth/passwords/reset",
                                                                "/api/v1/auth/refresh")
                                                .permitAll()

                                                // Listado público de aspirantes (y sus detalles)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/aspirantes",
                                                                "/api/v1/aspirantes/**")
                                                .permitAll()
                                                // Aspirantes: crear solicitud sin cuenta
                                                .requestMatchers(HttpMethod.POST, "/api/v1/aspirantes").permitAll()

                                                // Spots públicos (solo lectura)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/spots/**").permitAll()

                                                // Categorías y localidades públicas
                                                .requestMatchers(HttpMethod.GET, "/api/v1/categorias",
                                                                "/api/v1/localidades")
                                                .permitAll()

                                                // Perfiles públicos (solo lectura)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/perfil/**")
                                                .permitAll()

                                                // Monitoreo / Actuator
                                                .requestMatchers("/actuator/**", "/api/v1/actuator/**").permitAll()

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

                                                // RUTAS DE MODERADOR
                                                .requestMatchers("/api/v1/moderador/**").hasRole("MOD")
                                                // RUTAS DE ADMINISTRADOR
                                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                                                // LO DEMÁS bajo /api/v1
                                                // Cualquier otra ruta dentro de /api/v1 requiere autenticación
                                                .requestMatchers("/api/v1/**").authenticated()
                                                .anyRequest().authenticated())

                                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(jwtAuthenticationFilter, RateLimitFilter.class);

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
                                "http://127.0.0.1:3000"));
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
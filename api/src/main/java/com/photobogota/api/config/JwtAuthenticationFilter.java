package com.photobogota.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mongodb.lang.NonNull;

import java.io.IOException;

/**
 * Filtro de autenticación JWT que intercepta las solicitudes HTTP
 * y valida el token JWT presente en el header Authorization.
 * 
 * Este filtro:
 * 1. Extrae el token JWT del header "Authorization: Bearer <token>"
 * 2. Valida el token usando JwtService
 * 3. Carga los detalles del usuario desde UserDetailsService
 * 4. Establece la autenticación en el SecurityContext
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Método principal que se ejecuta en cada solicitud HTTP.
     * 
     * @param request     Solicitud HTTP entrante
     * @param response    Respuesta HTTP saliente
     * @param filterChain Cadena de filtros de Spring Security
     * @throws ServletException Si ocurre un error de servlet
     * @throws IOException      Si ocurre un error de E/S
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Extraer el header de autorización
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String nombreUsuario;

        // Si no hay header Authorization o no empieza con "Bearer ", continuar sin
        // autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extraer el token JWT (sin el prefijo "Bearer ")
        jwt = authHeader.substring(7);

        try {
            // 3. Extraer el nombre de usuario del token
            nombreUsuario = jwtService.extraerNombreUsuario(jwt);
            logger.info("DEBUG: Token recibido. Usuario extraído: " + nombreUsuario);

            // 4. Si hay un nombre de usuario y no hay autenticación actual, validar el
            // token
            if (nombreUsuario != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 5. Cargar los detalles del usuario desde la base de datos
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(nombreUsuario);
                logger.info("DEBUG: Usuario cargado de BD: " + userDetails.getUsername());

                // 6. Validar el token contra los detalles del usuario
                if (jwtService.esTokenValido(jwt, userDetails.getUsername())) {
                    logger.info("DEBUG: Token válido para usuario: " + userDetails.getUsername());

                    // 7. Crear token de autenticación con los detalles del usuario
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // No se necesita contraseña porque ya está validada con el token
                            userDetails.getAuthorities());

                    // 8. Establecer detalles adicionales de la solicitud
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 9. Establecer la autenticación en el SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("DEBUG: Autenticación establecida en SecurityContext");
                } else {
                    logger.warn("DEBUG: Token inválido para usuario: " + userDetails.getUsername());
                }
            } else if (nombreUsuario == null) {
                logger.warn("DEBUG: No se pudo extraer nombre de usuario del token");
            } else {
                logger.warn("DEBUG: Ya hay autenticación en SecurityContext");
            }
        } catch (Exception e) {
            // Si hay cualquier error con el token, continuar sin autenticar
            // El filtro no debe bloquear la solicitud, sino dejar que Spring Security lo
            // haga
            logger.warn("Token JWT inválido o expirado en ruta: " + request.getRequestURI()
                    + " - Error: " + e.getMessage());
            e.printStackTrace();
        }

        // 10. Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}

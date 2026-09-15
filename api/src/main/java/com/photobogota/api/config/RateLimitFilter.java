package com.photobogota.api.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de Rate Limiting que se ejecuta al inicio de la cadena de peticiones
 * HTTP.
 * Evalúa si el usuario ha excedido la cantidad de peticiones permitidas.
 */
@Component
@Order(1) // Ejecución previa a Spring Security
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Saltar peticiones OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = resolverIP(request);
        String uri = request.getRequestURI();

        // Obtiene o crea el bucket correspondiente a esta IP y URI
        Bucket bucket = rateLimitService.resolveBucket(ip, uri);

        // Intenta consumir 1 token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        // Header informativo para el cliente
        response.addHeader("X-RateLimit-Remaining",
                String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSegundos = probe.getNanosToWaitForRefill() / 1_000_000_000;

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.addHeader("Retry-After", String.valueOf(retryAfterSegundos));

            response.getWriter().write("""
                    {
                      "status": 429,
                      "error": "Too Many Requests",
                      "mensaje": "Demasiadas solicitudes. Intenta de nuevo en %d segundo(s)."
                    }
                    """.formatted(retryAfterSegundos));
        }
    }

    /**
     * Resuelve la dirección IP del cliente.
     * Permite soportar una cabecera personalizada para pruebas de carga (JMeter)
     * y las cabeceras estándar detrás de proxies (Caddy, Nginx, etc.).
     */
    private String resolverIP(HttpServletRequest request) {
        // 1. Cabecera personalizada para simular IPs aleatorias en pruebas con JMeter
        String testIp = request.getHeader("X-Test-Client-IP");
        if (testIp != null && !testIp.isBlank()) {
            return testIp;
        }

        // 2. Cabecera estándar de proxies (Caddy, Nginx, AWS ALB, etc.)
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }

        // 3. Cabecera X-Real-IP
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        // 4. Fallback a la IP directa de la conexión
        return request.getRemoteAddr();
    }
}
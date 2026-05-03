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
 * Se encarga de evaluar si el usuario ha excedido la cantidad de peticiones
 * permitidas por minuto.
 */
@Component
@Order(1) // Asegura que este filtro se ejecute antes de la cadena de seguridad (Spring
          // Security)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Saltar peticiones OPTIONS (usadas en preflight de CORS) para no bloquear las
        // llamadas del navegador
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = resolverIP(request);
        String uri = request.getRequestURI();

        // Obtiene o crea el bucket correspondiente a esta IP y URI
        Bucket bucket = rateLimitService.resolveBucket(ip, uri);

        // Intenta consumir 1 token y devuelve el resultado de la operación
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        // Agrega un header informativo para que el cliente sepa cuántas peticiones le
        // quedan
        response.addHeader("X-RateLimit-Remaining",
                String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            // Si hay tokens disponibles, deja continuar la petición
            filterChain.doFilter(request, response);
        } else {
            // Si se superaron los límites, calcula el tiempo restante para poder volver a
            // intentar
            long retryAfterSegundos = probe.getNanosToWaitForRefill() / 1_000_000_000;

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.addHeader("Retry-After", String.valueOf(retryAfterSegundos));

            // Envía un JSON descriptivo con el error
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
     * Resuelve y normaliza la dirección IP del cliente, soportando entornos detrás
     * de proxies o load balancers.
     *
     * @param request La petición HTTP.
     * @return La dirección IP real del cliente.
     */
    private String resolverIP(HttpServletRequest request) {
        // Soporta proxies / load balancers como Nginx, Railway, Render, etc.
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }

        // Soporta la cabecera X-Real-IP
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        // Fallback a la IP directa de la conexión
        return request.getRemoteAddr();
    }
}
package com.photobogota.api.config;

import java.io.IOException;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.photobogota.api.dto.EstadoMantenimientoDTO;
import com.photobogota.api.service.IMantenimientoService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Filtro que bloquea las peticiones mientras el sistema está en una ventana
 * de mantenimiento programada (HU #47), devolviendo 503 con el mensaje del
 * aviso. Deja pasar siempre las rutas de administración (para que el admin
 * pueda seguir gestionando/cancelando el mantenimiento), login/refresh, el
 * propio endpoint de estado, y documentación.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class MantenimientoFilter extends OncePerRequestFilter {

    private final IMantenimientoService mantenimientoService;

    private static final List<String> PREFIJOS_EXENTOS = List.of(
            "/api/v1/admin",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/me",
            "/api/v1/mantenimiento/estado",
            "/swagger-ui",
            "/v3/api-docs",
            "/api/v1/actuator",
            "/actuator");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || esRutaExenta(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        EstadoMantenimientoDTO estado = mantenimientoService.obtenerEstado();
        if (estado.isEnMantenimiento()) {
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {
                      "status": 503,
                      "error": "Servicio no disponible",
                      "mensaje": "%s",
                      "fechaFin": "%s"
                    }
                    """.formatted(escapar(estado.getMensaje()), estado.getFechaFin()));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean esRutaExenta(String uri) {
        return PREFIJOS_EXENTOS.stream().anyMatch(uri::startsWith);
    }

    private String escapar(String texto) {
        return texto == null ? "" : texto.replace("\"", "\\\"");
    }
}

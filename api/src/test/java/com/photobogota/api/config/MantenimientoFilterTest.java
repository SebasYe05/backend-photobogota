package com.photobogota.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.photobogota.api.dto.EstadoMantenimientoDTO;
import com.photobogota.api.service.IMantenimientoService;

import jakarta.servlet.FilterChain;

@ExtendWith(MockitoExtension.class)
class MantenimientoFilterTest {

    @Mock
    private IMantenimientoService mantenimientoService;

    @Test
    void sinMantenimiento_continuaLaCadena() throws Exception {
        when(mantenimientoService.obtenerEstado()).thenReturn(
                EstadoMantenimientoDTO.builder().enMantenimiento(false).build());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/spots");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(200);

        new MantenimientoFilter(mantenimientoService).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void enMantenimiento_devuelve503ConMensajeYFechaFin() throws Exception {
        when(mantenimientoService.obtenerEstado()).thenReturn(EstadoMantenimientoDTO.builder()
                .enMantenimiento(true)
                .mensaje("Estamos de mantenimiento")
                .fechaFin(LocalDateTime.of(2026, 9, 2, 23, 0))
                .build());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/spots");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mockChain();

        new MantenimientoFilter(mantenimientoService).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString())
                .contains("Estamos de mantenimiento")
                .contains("2026-09-02T23:00");
        org.mockito.Mockito.verify(chain, org.mockito.Mockito.never()).doFilter(request, response);
    }

    @Test
    void enMantenimiento_rutasExentasDebenPasar() throws Exception {
        List<String> rutasExentas = List.of(
                "/api/v1/admin/mantenimiento",
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/mantenimiento/estado",
                "/swagger-ui/index.html",
                "/v3/api-docs",
                "/actuator/health");

        for (String ruta : rutasExentas) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", ruta);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(200);

            new MantenimientoFilter(mantenimientoService).doFilter(request, response, chain);

            assertThat(response.getStatus())
                    .withFailMessage("La ruta exenta %s no debería bloquearse", ruta)
                    .isEqualTo(200);
        }
    }

    @Test
    void peticionOptions_siemprePasa() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/spots");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(200);

        new MantenimientoFilter(mantenimientoService).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    private FilterChain mockChain() {
        return org.mockito.Mockito.mock(FilterChain.class);
    }
}
package com.photobogota.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.photobogota.api.dto.PuntosResponseDTO;
import com.photobogota.api.service.IPuntosService;

import org.junit.jupiter.api.Test;

class AdminPuntosControllerTest extends ControllerTestSupport {

    private final IPuntosService puntosService = mock(IPuntosService.class);
    private final AdminPuntosController controller = new AdminPuntosController(puntosService);

    @Test
    void obtenerConfig_devuelve200() throws Exception {
        when(puntosService.obtenerConfig()).thenReturn(Map.of("crearSpot", "10"));

        mvc(controller)
                .perform(get("/api/v1/admin/puntos/config").with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void actualizarConfig_devuelve200() throws Exception {
        when(puntosService.actualizarConfig(any())).thenReturn(Map.of("crearSpot", "15"));

        mvc(controller)
                .perform(json(put("/api/v1/admin/puntos/config"),
                        "{\"crearSpot\":\"15\"}")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void ajustarPuntos_devuelve200() throws Exception {
        when(puntosService.ajustarPuntosPorId(eq("u1"), eq(50), eq("Bonus")))
                .thenReturn(mock(PuntosResponseDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/admin/puntos/usuarios/u1/puntos"),
                        "{\"delta\":50,\"motivo\":\"Bonus\"}")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void ajustarPuntos_sinDelta_devuelve400() throws Exception {
        mvc(controller)
                .perform(json(post("/api/v1/admin/puntos/usuarios/u1/puntos"),
                        "{\"motivo\":\"Bonus\"}")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isBadRequest());
    }
}
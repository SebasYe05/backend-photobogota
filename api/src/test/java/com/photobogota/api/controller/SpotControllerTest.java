package com.photobogota.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.photobogota.api.dto.SpotResumenDTO;
import com.photobogota.api.dto.SpotResponseDTO;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.service.SpotService;

import org.junit.jupiter.api.Test;

class SpotControllerTest extends ControllerTestSupport {

    private final SpotService spotService = mock(SpotService.class);
    private final SpotController controller = new SpotController(spotService);

    @Test
    void listarSpot_publico_devuelve200() throws Exception {
        when(spotService.obtenerTodos(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mock(SpotResumenDTO.class)));

        mvc(controller)
                .perform(get("/api/v1/spots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listarSpot_conFiltrosYPasandoLosParametros() throws Exception {
        when(spotService.obtenerTodos(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mvc(controller)
                .perform(get("/api/v1/spots")
                        .param("categoria", "Paisaje urbano")
                        .param("localidad", "Chapinero")
                        .param("tipo", "SPOT")
                        .param("nombre", "parque")
                        .param("mios", "true")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());

        verify(spotService).obtenerTodos(
                eq("Paisaje urbano"), eq("Chapinero"), eq("SPOT"), eq("parque"),
                eq(Boolean.TRUE), eq("juanromero"));
    }

    @Test
    void obtenerSpot_devuelve200() throws Exception {
        when(spotService.obtenerPorId("s1")).thenReturn(mock(SpotResponseDTO.class));

        mvc(controller)
                .perform(get("/api/v1/spots/s1"))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerSpot_inexistente_devuelve404() throws Exception {
        when(spotService.obtenerPorId("s1"))
                .thenThrow(new ResourceNotFoundException("Spot no encontrado con id: s1"));

        mvc(controller)
                .perform(get("/api/v1/spots/s1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Spot no encontrado con id: s1"));
    }

    @Test
    void crearSpot_devuelve201() throws Exception {
        when(spotService.crearSpot(any(), eq("juanromero"), eq("MIEMBRO")))
                .thenReturn(mock(SpotResponseDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/spots"),
                        "{\"nombre\":\"Parque Central\",\"latitud\":4.6097,\"longitud\":-74.0817,"
                                + "\"direccion\":\"Calle 123\",\"categoria\":\"Parque\","
                                + "\"localidad\":\"Kennedy\",\"descripcion\":\"Lindo parque\","
                                + "\"tipo\":\"SPOT\"}")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isCreated());

        verify(spotService).crearSpot(any(), eq("juanromero"), eq("MIEMBRO"));
    }

    @Test
    void crearSpot_conCuerpoInvalido_devuelve400() throws Exception {
        mvc(controller)
                .perform(json(post("/api/v1/spots"),
                        "{\"nombre\":\"\",\"categoria\":\"Parque\"}")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void agregarResena_devuelve200() throws Exception {
        when(spotService.agregarResena(eq("s1"), any(), eq("juanromero")))
                .thenReturn(mock(SpotResponseDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/spots/s1/resenas"),
                        "{\"rating\":5,\"comentario\":\"Excelente spot\"}")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());

        verify(spotService).agregarResena(eq("s1"), any(), eq("juanromero"));
    }
}
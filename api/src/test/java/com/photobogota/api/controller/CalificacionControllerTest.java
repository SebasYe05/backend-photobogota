package com.photobogota.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.photobogota.api.dto.CalificacionResponseDTO;
import com.photobogota.api.service.ICalificacionService;

import org.junit.jupiter.api.Test;

class CalificacionControllerTest extends ControllerTestSupport {

    private final ICalificacionService calificacionService = mock(ICalificacionService.class);
    private final CalificacionController controller = new CalificacionController(calificacionService);

    @Test
    void crearCalificacion_devuelve201() throws Exception {
        when(calificacionService.crearCalificacion(eq("s1"), any(), eq("juanromero")))
                .thenReturn(mock(CalificacionResponseDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/spots/s1/calificaciones"),
                        "{\"estrellas\":5,\"comentario\":\"Excelente spot\"}")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isCreated());
    }

    @Test
    void crearCalificacion_conEstrellasFueraDeRango_devuelve400() throws Exception {
        mvc(controller)
                .perform(json(post("/api/v1/spots/s1/calificaciones"),
                        "{\"estrellas\":10}")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarPorSpot_devuelve200() throws Exception {
        when(calificacionService.listarPorSpot("s1")).thenReturn(List.of(mock(CalificacionResponseDTO.class)));

        mvc(controller)
                .perform(get("/api/v1/spots/s1/calificaciones"))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerCalificacion_devuelve200() throws Exception {
        when(calificacionService.obtenerPorId("c1")).thenReturn(mock(CalificacionResponseDTO.class));

        mvc(controller)
                .perform(get("/api/v1/spots/s1/calificaciones/c1"))
                .andExpect(status().isOk());
    }

    @Test
    void modificarCalificacion_devuelve200() throws Exception {
        when(calificacionService.modificarCalificacion(eq("s1"), eq("c1"), any(), eq("juanromero")))
                .thenReturn(mock(CalificacionResponseDTO.class));

        mvc(controller)
                .perform(json(put("/api/v1/spots/s1/calificaciones/c1"),
                        "{\"estrellas\":4,\"comentario\":\"Mejoró mi opinión\"}")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }
}
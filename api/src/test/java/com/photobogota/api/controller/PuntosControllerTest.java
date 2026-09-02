package com.photobogota.api.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.photobogota.api.dto.PuntosResponseDTO;
import com.photobogota.api.service.IPuntosService;

import org.junit.jupiter.api.Test;

class PuntosControllerTest extends ControllerTestSupport {

    private final IPuntosService puntosService = mock(IPuntosService.class);
    private final PuntosController controller = new PuntosController(puntosService);

    @Test
    void obtenerMisPuntos_devuelve200() throws Exception {
        when(puntosService.obtenerPuntos("juanromero")).thenReturn(mock(PuntosResponseDTO.class));

        mvc(controller)
                .perform(get("/api/v1/usuarios/me/puntos").with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }
}
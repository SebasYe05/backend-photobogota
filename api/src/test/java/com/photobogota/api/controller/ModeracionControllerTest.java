package com.photobogota.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.photobogota.api.dto.SancionDTO;
import com.photobogota.api.service.IFiltroContenidoService;

import org.junit.jupiter.api.Test;

class ModeracionControllerTest extends ControllerTestSupport {

    private final IFiltroContenidoService filtroContenidoService = mock(IFiltroContenidoService.class);
    private final ModeracionController controller = new ModeracionController(filtroContenidoService);

    @Test
    void obtenerMiSancion_devuelve200() throws Exception {
        when(filtroContenidoService.obtenerSancionActual("juanromero")).thenReturn(mock(SancionDTO.class));

        mvc(controller)
                .perform(get("/api/v1/moderacion/mi-sancion")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }

    @Test
    void apelarBan_devuelve200ConMensaje() throws Exception {
        mvc(controller)
                .perform(json(post("/api/v1/moderacion/mi-sancion/apelar"),
                        "{\"motivo\":\"Fue un error\"}")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje")
                        .value("Apelación enviada correctamente. Un administrador la revisará."));

        org.mockito.Mockito.verify(filtroContenidoService).apelarBan(eq("juanromero"), any());
    }

    @Test
    void apelarBan_sinMotivo_devuelve400() throws Exception {
        mvc(controller)
                .perform(json(post("/api/v1/moderacion/mi-sancion/apelar"),
                        "{}")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isBadRequest());
    }
}
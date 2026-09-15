package com.photobogota.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.photobogota.api.dto.PalabraProhibidaDTO;
import com.photobogota.api.dto.RegistroModeracionDTO;
import com.photobogota.api.service.IFiltroContenidoService;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

class AdminModeracionControllerTest extends ControllerTestSupport {

    private final IFiltroContenidoService filtroContenidoService = mock(IFiltroContenidoService.class);
    private final AdminModeracionController controller = new AdminModeracionController(filtroContenidoService);

    @Test
    void listarPalabras_devuelve200() throws Exception {
        when(filtroContenidoService.listarPalabras()).thenReturn(java.util.Collections.emptyList());

        mvc(controller)
                .perform(get("/api/v1/admin/moderacion/palabras").with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void crearPalabra_devuelve201() throws Exception {
        when(filtroContenidoService.crearPalabra(any(), eq("admin")))
                .thenReturn(mock(PalabraProhibidaDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/admin/moderacion/palabras"),
                        "{\"texto\":\"groseria\",\"tipo\":\"PALABRA\",\"activo\":true}")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isCreated());
    }

    @Test
    void actualizarPalabra_devuelve200() throws Exception {
        when(filtroContenidoService.actualizarPalabra(eq("p1"), any()))
                .thenReturn(mock(PalabraProhibidaDTO.class));

        mvc(controller)
                .perform(json(put("/api/v1/admin/moderacion/palabras/p1"),
                        "{\"texto\":\"groseria\",\"activo\":false}")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void eliminarPalabra_devuelve204() throws Exception {
        mvc(controller)
                .perform(delete("/api/v1/admin/moderacion/palabras/p1")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void togglePalabra_devuelve200() throws Exception {
        when(filtroContenidoService.togglePalabra("p1")).thenReturn(mock(PalabraProhibidaDTO.class));

        mvc(controller)
                .perform(patch("/api/v1/admin/moderacion/palabras/p1/toggle")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void listarHistorial_devuelve200() throws Exception {
        when(filtroContenidoService.listarHistorial(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(),
                        org.springframework.data.domain.PageRequest.of(0, 10), 0));

        mvc(controller)
                .perform(get("/api/v1/admin/moderacion/historial")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void listarApelacionesPendientes_devuelve200() throws Exception {
        when(filtroContenidoService.listarApelacionesPendientes())
                .thenReturn(List.of(mock(RegistroModeracionDTO.class)));

        mvc(controller)
                .perform(get("/api/v1/admin/moderacion/apelaciones").with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void resolverApelacion_devuelve200() throws Exception {
        when(filtroContenidoService.resolverApelacion(eq("r1"), any(), eq("admin")))
                .thenReturn(mock(RegistroModeracionDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/admin/moderacion/apelaciones/r1/resolver"),
                        "{\"aprobar\":true,\"respuesta\":\"Cuenta reactivada\"}")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void resolverApelacion_sinRespuesta_devuelve400() throws Exception {
        mvc(controller)
                .perform(json(post("/api/v1/admin/moderacion/apelaciones/r1/resolver"),
                        "{\"aprobar\":true}")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isBadRequest());
    }
}
package com.photobogota.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.photobogota.api.dto.EstadoMantenimientoDTO;
import com.photobogota.api.dto.MantenimientoResponseDTO;
import com.photobogota.api.service.IMantenimientoService;

import org.junit.jupiter.api.Test;

class MantenimientoControllerTest extends ControllerTestSupport {

    private final IMantenimientoService mantenimientoService = mock(IMantenimientoService.class);
    private final MantenimientoController controller = new MantenimientoController(mantenimientoService);

    @Test
    void obtenerEstado_devuelve200() throws Exception {
        when(mantenimientoService.obtenerEstado()).thenReturn(mock(EstadoMantenimientoDTO.class));

        mvc(controller)
                .perform(get("/api/v1/mantenimiento/estado"))
                .andExpect(status().isOk());
    }

    @Test
    void listarProgramados_devuelve200() throws Exception {
        when(mantenimientoService.listarProgramados()).thenReturn(List.of(mock(MantenimientoResponseDTO.class)));

        mvc(controller)
                .perform(get("/api/v1/admin/mantenimiento").with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void programar_devuelve201() throws Exception {
        when(mantenimientoService.programar(any(), eq("admin")))
                .thenReturn(mock(MantenimientoResponseDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/admin/mantenimiento"),
                        "{\"fechaInicio\":\"2027-01-01T10:00:00\",\"fechaFin\":\"2027-01-01T12:00:00\","
                                + "\"motivo\":\"Actualización de infraestructura\"}")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isCreated());
    }

    @Test
    void programar_conFechasIncorrectas_devuelve400() throws Exception {
        mvc(controller)
                .perform(json(post("/api/v1/admin/mantenimiento"),
                        "{\"fechaInicio\":\"2027-01-01T10:00:00\",\"fechaFin\":\"2027-01-01T12:00:00\","
                                + "\"motivo\":\"\"}")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void programar_cuandoElServicioLanzaIllegalArgument_devuelve400() throws Exception {
        when(mantenimientoService.programar(any(), eq("admin")))
                .thenThrow(new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio"));

        mvc(controller)
                .perform(json(post("/api/v1/admin/mantenimiento"),
                        "{\"fechaInicio\":\"2027-01-01T10:00:00\",\"fechaFin\":\"2026-01-01T12:00:00\","
                                + "\"motivo\":\"Prueba\"}")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("La fecha de fin no puede ser anterior a la de inicio"));
    }

    @Test
    void cancelar_devuelve204() throws Exception {
        mvc(controller)
                .perform(delete("/api/v1/admin/mantenimiento/m1").with(autenticado("admin", "ADMIN")))
                .andExpect(status().isNoContent());
    }
}
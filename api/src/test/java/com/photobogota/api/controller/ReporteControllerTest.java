package com.photobogota.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.photobogota.api.dto.ReporteResponseDTO;
import com.photobogota.api.model.Rol;
import com.photobogota.api.service.IReporteService;

import org.junit.jupiter.api.Test;

class ReporteControllerTest extends ControllerTestSupport {

    private final IReporteService reporteService = mock(IReporteService.class);
    private final ReporteController controller = new ReporteController(reporteService);

    @Test
    void crearReporte_devuelve201() throws Exception {
        when(reporteService.crearReporte(any(), eq("juanromero")))
                .thenReturn(mock(ReporteResponseDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/reportes"),
                        "{\"categoria\":\"SPAM\",\"descripcion\":\"Spot con horarios desactualizados\"}")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isCreated());
    }

    @Test
    void crearReporte_sinDescripcion_devuelve400() throws Exception {
        mvc(controller)
                .perform(json(post("/api/v1/reportes"),
                        "{\"categoria\":\"SPAM\"}")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarMisReportes_devuelve200() throws Exception {
        when(reporteService.listarMisReportes("juanromero")).thenReturn(java.util.List.of());

        mvc(controller)
                .perform(get("/api/v1/reportes/mios").with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerReporte_devuelve200() throws Exception {
        when(reporteService.obtenerPorId("r1")).thenReturn(mock(ReporteResponseDTO.class));

        mvc(controller)
                .perform(get("/api/v1/reportes/r1").with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }

    @Test
    void listarAsignadosAModerador_devuelve200() throws Exception {
        when(reporteService.listarPorRolAsignado(Rol.MOD)).thenReturn(java.util.List.of());

        mvc(controller)
                .perform(get("/api/v1/reportes/asignados/moderador").with(autenticado("mod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void listarAsignadosAAdmin_devuelve200() throws Exception {
        when(reporteService.listarPorRolAsignado(Rol.ADMIN)).thenReturn(java.util.List.of());

        mvc(controller)
                .perform(get("/api/v1/reportes/asignados/admin").with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerDashboard_comoSocio_devuelve200() throws Exception {
        when(reporteService.obtenerDashboard(eq(Rol.SOCIO), anyString(), any(), any(), any(), any(),
                anyBoolean(), anyString())).thenReturn(java.util.List.of());

        mvc(controller)
                .perform(get("/api/v1/reportes/dashboard")
                        .param("estado", "EN_REVISION")
                        .with(autenticado("socio1", "SOCIO")))
                .andExpect(status().isOk());
    }

    @Test
    void cambiarEstado_devuelve200() throws Exception {
        when(reporteService.cambiarEstado(eq("r1"), any(), eq("mod"), eq(Rol.MOD)))
                .thenReturn(mock(ReporteResponseDTO.class));

        mvc(controller)
                .perform(json(patch("/api/v1/reportes/r1/estado"),
                        "{\"estado\":\"EN_REVISION\",\"observacion\":\"En revisión\"}")
                        .with(autenticado("mod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void cambiarEstado_sinEstado_devuelve400() throws Exception {
        mvc(controller)
                .perform(json(patch("/api/v1/reportes/r1/estado"),
                        "{\"observacion\":\"\"}")
                        .with(autenticado("mod", "MOD")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void escalarReporte_devuelve200() throws Exception {
        when(reporteService.escalarReporte(eq("r1"), any(), eq("mod"), eq(Rol.MOD)))
                .thenReturn(mock(ReporteResponseDTO.class));

        mvc(controller)
                .perform(json(patch("/api/v1/reportes/r1/escalar"),
                        "{\"motivo\":\"Requiere revisión de admin\"}")
                        .with(autenticado("mod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void listarPendientesValidacion_devuelve200() throws Exception {
        when(reporteService.listarPendientesValidacion()).thenReturn(java.util.List.of());

        mvc(controller)
                .perform(get("/api/v1/reportes/pendientes-validacion").with(autenticado("mod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void validarReporte_devuelve200() throws Exception {
        when(reporteService.validarReporte(eq("r1"), any(), eq("mod")))
                .thenReturn(mock(ReporteResponseDTO.class));

        mvc(controller)
                .perform(json(patch("/api/v1/reportes/r1/validar"),
                        "{\"aprobado\":true,\"observacion\":\"Verificado\"}")
                        .with(autenticado("mod", "MOD")))
                .andExpect(status().isOk());
    }
}
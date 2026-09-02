package com.photobogota.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.photobogota.api.dto.MetricasEliminacionDTO;
import com.photobogota.api.dto.SolicitudEliminacionAdminDTO;
import com.photobogota.api.service.IAdminEliminacionCuentaService;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

class AdminEliminacionControllerTest extends ControllerTestSupport {

    private final IAdminEliminacionCuentaService service = mock(IAdminEliminacionCuentaService.class);
    private final AdminEliminacionController controller = new AdminEliminacionController(service);

    @Test
    void listarSolicitudes_devuelve200() throws Exception {
        when(service.listarSolicitudes(isNull(), any())).thenReturn(new PageImpl<>(java.util.List.of(),
                        org.springframework.data.domain.PageRequest.of(0, 10), 0));

        mvc(controller)
                .perform(get("/api/v1/admin/eliminaciones").with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerDetalle_devuelve200() throws Exception {
        when(service.obtenerDetalle("e1")).thenReturn(mock(SolicitudEliminacionAdminDTO.class));

        mvc(controller)
                .perform(get("/api/v1/admin/eliminaciones/e1").with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void procesarSinCuerpo_devuelve200() throws Exception {
        when(service.procesarInmediatamente("e1", "admin", null)).thenReturn("Cuenta anonimizada");

        mvc(controller)
                .perform(post("/api/v1/admin/eliminaciones/e1/procesar")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void procesarConObservacion_devuelve200() throws Exception {
        when(service.procesarInmediatamente(eq("e1"), eq("admin"), any()))
                .thenReturn("Cuenta anonimizada");

        mvc(controller)
                .perform(json(post("/api/v1/admin/eliminaciones/e1/procesar"),
                        "{\"observacion\":\"Verificado por identidad\"}")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void rechazar_devuelve200() throws Exception {
        when(service.rechazarSolicitud(anyString(), eq("admin"), any())).thenReturn("Solicitud rechazada");

        mvc(controller)
                .perform(post("/api/v1/admin/eliminaciones/e1/rechazar")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerMetricas_devuelve200() throws Exception {
        when(service.obtenerMetricas()).thenReturn(mock(MetricasEliminacionDTO.class));

        mvc(controller)
                .perform(get("/api/v1/admin/eliminaciones/metricas").with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }
}
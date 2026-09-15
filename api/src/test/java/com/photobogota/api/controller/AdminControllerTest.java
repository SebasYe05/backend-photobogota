package com.photobogota.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.photobogota.api.config.RateLimitService;
import com.photobogota.api.dto.RegistroResponseDTO;
import com.photobogota.api.dto.UsuarioListResponseDTO;
import com.photobogota.api.service.IAdminService;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

class AdminControllerTest extends ControllerTestSupport {

    private final IAdminService adminService = mock(IAdminService.class);
    private final RateLimitService rateLimitService = mock(RateLimitService.class);
    private final AdminController controller = new AdminController(adminService, rateLimitService);

    @Test
    void crearUsuario_devuelve201() throws Exception {
        when(adminService.crearUsuario(any())).thenReturn(mock(RegistroResponseDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/admin/usuarios"),
                        "{\"nombresCompletos\":\"Daniel Cruz\",\"email\":\"daniel@example.com\","
                                + "\"nombreUsuario\":\"daniel\",\"contrasena\":\"Seguro123.\","
                                + "\"fechaNacimiento\":\"2004-11-05\",\"rol\":\"SOCIO\"}")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isCreated());

        verify(adminService).crearUsuario(any());
    }

    @Test
    void crearUsuario_conCuerpoInvalido_devuelve400() throws Exception {
        mvc(controller)
                .perform(json(post("/api/v1/admin/usuarios"),
                        "{\"rol\":\"SOCIO\"}")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarUsuarios_devuelve200() throws Exception {
        when(adminService.listarUsuarios(any()))
                .thenReturn(new PageImpl<>(java.util.List.of(),
                        org.springframework.data.domain.PageRequest.of(0, 10), 0));

        mvc(controller)
                .perform(get("/api/v1/admin/usuarios")
                        .param("page", "0").param("size", "5")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void actualizarEstadoUsuario_devuelve200() throws Exception {
        mvc(controller)
                .perform(patch("/api/v1/admin/usuarios/u1/estado")
                        .param("activo", "false")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());

        verify(adminService).actualizarEstadoUsuario("u1", false);
    }

    @Test
    void eliminarUsuario_devuelve204() throws Exception {
        mvc(controller)
                .perform(delete("/api/v1/admin/usuarios/u1")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isNoContent());

        verify(adminService).eliminarUsuario("u1");
    }

    @Test
    void estadisticasRateLimit_devuelve200() throws Exception {
        when(rateLimitService.obtenerEstadisticas())
                .thenReturn(Map.of("totalIPs", 3, "activos", 2));

        mvc(controller)
                .perform(get("/api/v1/admin/rate-limit/stats")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }
}
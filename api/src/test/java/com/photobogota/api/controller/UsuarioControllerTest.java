package com.photobogota.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import com.photobogota.api.dto.CalificacionResponseDTO;
import com.photobogota.api.dto.CambiarContrasenaResponseDTO;
import com.photobogota.api.dto.EstadoEliminacionDTO;
import com.photobogota.api.dto.PerfilUsuarioDTO;
import com.photobogota.api.dto.SpotResumenDTO;
import com.photobogota.api.service.IEliminacionCuentaService;
import com.photobogota.api.service.IUsuarioService;

import org.junit.jupiter.api.Test;

class UsuarioControllerTest extends ControllerTestSupport {

    private final IUsuarioService usuarioService = mock(IUsuarioService.class);
    private final IEliminacionCuentaService eliminacionCuentaService = mock(IEliminacionCuentaService.class);
    private final UsuarioController controller = new UsuarioController(usuarioService, eliminacionCuentaService);

    @Test
    void obtenerPerfilPublico_devuelve200() throws Exception {
        when(usuarioService.obtenerPerfil("fotografo")).thenReturn(mock(PerfilUsuarioDTO.class));

        mvc(controller)
                .perform(get("/api/v1/usuarios/perfil/fotografo"))
                .andExpect(status().isOk());
    }

    @Test
    void editarPerfil_devuelve200() throws Exception {
        when(usuarioService.editarPerfil(eq("juanromero"), any())).thenReturn(mock(PerfilUsuarioDTO.class));

        mvc(controller)
                .perform(json(put("/api/v1/usuarios/perfil"),
                        "{\"nombresCompletos\":\"Juan Romero\"}")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());

        verify(usuarioService).editarPerfil(eq("juanromero"), any());
    }

    @Test
    void cambiarContrasena_devuelve200() throws Exception {
        when(usuarioService.cambiarContrasena(eq("juanromero"), any()))
                .thenReturn(mock(CambiarContrasenaResponseDTO.class));

        mvc(controller)
                .perform(json(patch("/api/v1/usuarios/me/password"),
                        "{\"contrasenaActual\":\"Vieja123.\",\"nuevaContrasena\":\"Nueva123.\","
                                + "\"confirmarContrasena\":\"Nueva123.\"}")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }

    @Test
    void cambiarContrasena_conCuerpoInvalido_devuelve400() throws Exception {
        mvc(controller)
                .perform(json(patch("/api/v1/usuarios/me/password"),
                        "{\"contrasenaActual\":\"\"}")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void misSpots_devuelve200() throws Exception {
        when(usuarioService.obtenerSpotsDeUsuario("juanromero")).thenReturn(List.of(mock(SpotResumenDTO.class)));

        mvc(controller)
                .perform(get("/api/v1/usuarios/me/spots").with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void misResenas_devuelve200() throws Exception {
        when(usuarioService.obtenerResenasDeUsuario("juanromero"))
                .thenReturn(List.of(mock(CalificacionResponseDTO.class)));

        mvc(controller)
                .perform(get("/api/v1/usuarios/me/resenas").with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void misGuardados_devuelve200() throws Exception {
        when(usuarioService.obtenerGuardados("juanromero")).thenReturn(List.of(mock(SpotResumenDTO.class)));

        mvc(controller)
                .perform(get("/api/v1/usuarios/me/guardados").with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }

    @Test
    void guardarSpot_devuelve200() throws Exception {
        when(usuarioService.guardarSpot("juanromero", "s1")).thenReturn(mock(SpotResumenDTO.class));

        mvc(controller)
                .perform(post("/api/v1/usuarios/me/guardados/s1").with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }

    @Test
    void quitarGuardado_devuelve204() throws Exception {
        mvc(controller)
                .perform(delete("/api/v1/usuarios/me/guardados/s1").with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isNoContent());

        verify(usuarioService).quitarGuardado("juanromero", "s1");
    }

    @Test
    void spotsDeUsuarioPublico_devuelve200() throws Exception {
        when(usuarioService.obtenerSpotsDeUsuario("otro")).thenReturn(List.of());

        mvc(controller)
                .perform(get("/api/v1/usuarios/otro/spots"))
                .andExpect(status().isOk());
    }

    @Test
    void resenasDeUsuarioPublico_devuelve200() throws Exception {
        when(usuarioService.obtenerResenasDeUsuario("otro")).thenReturn(null);

        mvc(controller)
                .perform(get("/api/v1/usuarios/otro/resenas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void solicitarEliminacion_sinCuerpo_devuelve200() throws Exception {
        when(eliminacionCuentaService.solicitarEliminacion(eq("juanromero"), any()))
                .thenReturn("Código enviado por correo");

        mvc(controller)
                .perform(post("/api/v1/usuarios/me/eliminacion/solicitar")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Código enviado por correo"));

        verify(eliminacionCuentaService).solicitarEliminacion(eq("juanromero"), any());
    }

    @Test
    void confirmarEliminacion_devuelve200() throws Exception {
        when(eliminacionCuentaService.confirmarEliminacion(eq("juanromero"), any()))
                .thenReturn("Eliminación programada");

        mvc(controller)
                .perform(json(post("/api/v1/usuarios/me/eliminacion/confirmar"),
                        "{\"codigo\":\"482913\"}")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Eliminación programada"));
    }

    @Test
    void confirmarEliminacion_sinCodigo_devuelve400() throws Exception {
        mvc(controller)
                .perform(json(post("/api/v1/usuarios/me/eliminacion/confirmar"),
                        "{}")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelarEliminacion_devuelve200() throws Exception {
        when(eliminacionCuentaService.cancelarEliminacion("juanromero"))
                .thenReturn("Eliminación cancelada");

        mvc(controller)
                .perform(post("/api/v1/usuarios/me/eliminacion/cancelar")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }

    @Test
    void estadoEliminacion_devuelve200() throws Exception {
        when(eliminacionCuentaService.obtenerEstado("juanromero")).thenReturn(mock(EstadoEliminacionDTO.class));

        mvc(controller)
                .perform(get("/api/v1/usuarios/me/eliminacion/estado")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }
}
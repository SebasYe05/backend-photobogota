package com.photobogota.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.photobogota.api.dto.ContadorNotificacionesDTO;
import com.photobogota.api.dto.NotificacionResponseDTO;
import com.photobogota.api.dto.PreferenciasNotificacionDTO;
import com.photobogota.api.model.AlcanceNotificacion;
import com.photobogota.api.service.INotificacionService;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

class NotificacionControllerTest extends ControllerTestSupport {

    private final INotificacionService notificacionService = mock(INotificacionService.class);
    private final NotificacionController controller = new NotificacionController(notificacionService);

    @Test
    void listarMisNotificaciones_devuelve200() throws Exception {
        when(notificacionService.listarMisNotificaciones(anyString(), any(), anyBoolean()))
                .thenReturn(new PageImpl<>(java.util.List.of(),
                        org.springframework.data.domain.PageRequest.of(0, 20), 0));

        mvc(controller)
                .perform(get("/api/v1/notificaciones").with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }

    @Test
    void contarNoLeidas_devuelve200() throws Exception {
        when(notificacionService.contarNoLeidas("juanromero")).thenReturn(3L);

        mvc(controller)
                .perform(get("/api/v1/notificaciones/no-leidas/contador")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }

    @Test
    void marcarLeida_devuelve204() throws Exception {
        mvc(controller)
                .perform(patch("/api/v1/notificaciones/n1/leida").with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isNoContent());
    }

    @Test
    void marcarTodasLeidas_devuelve204() throws Exception {
        mvc(controller)
                .perform(patch("/api/v1/notificaciones/leer-todas")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminarNotificacion_devuelve204() throws Exception {
        mvc(controller)
                .perform(delete("/api/v1/notificaciones/n1").with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isNoContent());
    }

    @Test
    void obtenerPreferencias_devuelve200() throws Exception {
        when(notificacionService.obtenerPreferencias("juanromero"))
                .thenReturn(mock(PreferenciasNotificacionDTO.class));

        mvc(controller)
                .perform(get("/api/v1/notificaciones/preferencias")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }

    @Test
    void actualizarPreferencias_devuelve200() throws Exception {
        when(notificacionService.actualizarPreferencias(anyString(), any()))
                .thenReturn(mock(PreferenciasNotificacionDTO.class));

        mvc(controller)
                .perform(json(put("/api/v1/notificaciones/preferencias"),
                        "{\"notificacionesActivas\":false}")
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }

    @Test
    void enviarNotificacion_devuelve202() throws Exception {
        mvc(controller)
                .perform(json(post("/api/v1/notificaciones/enviar"),
                        "{\"titulo\":\"Aviso\",\"mensaje\":\"Mantenimiento programado\","
                                + "\"alcance\":\"TODOS\"}")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isAccepted());

        org.mockito.Mockito.verify(notificacionService).enviarNotificacionManual(any(), eq("admin"), eq("ADMIN"));
    }

    @Test
    void enviarNotificacion_sinCuerpoValido_devuelve400() throws Exception {
        mvc(controller)
                .perform(json(post("/api/v1/notificaciones/enviar"),
                        "{\"titulo\":\"\",\"mensaje\":\"\"}")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isBadRequest());
    }
}
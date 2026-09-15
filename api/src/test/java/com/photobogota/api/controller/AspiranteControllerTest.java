package com.photobogota.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.photobogota.api.dto.AspiranteResponseDTO;
import com.photobogota.api.dto.EstadisticasAspiranteDTO;
import com.photobogota.api.model.EstadoAspirante;
import com.photobogota.api.service.IAspiranteService;

import org.junit.jupiter.api.Test;

class AspiranteControllerTest extends ControllerTestSupport {

    private final IAspiranteService aspiranteService = mock(IAspiranteService.class);
    private final AspiranteController controller = new AspiranteController(aspiranteService);

    private static final String SOLICITUD_VALIDA =
            "{\"nombres\":\"Juan Sebastian\",\"apellidos\":\"Romero Ramirez\","
                    + "\"email\":\"juan.romero@example.com\",\"nit\":\"123456789\","
                    + "\"fechaNacimiento\":\"1990-01-01\",\"rutaArchivo\":\"/uploads/cedula.pdf\","
                    + "\"tipoArchivo\":\"pdf\"}";

    @Test
    void crearSolicitud_devuelve201() throws Exception {
        when(aspiranteService.crearSolicitud(any())).thenReturn(mock(AspiranteResponseDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/aspirantes"), SOLICITUD_VALIDA))
                .andExpect(status().isCreated());
    }

    @Test
    void crearSolicitud_conCuerpoInvalido_devuelve400() throws Exception {
        mvc(controller)
                .perform(json(post("/api/v1/aspirantes"),
                        "{\"nombres\":\"Juan\",\"email\":\"correo-invalido\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerPorId_devuelve200() throws Exception {
        when(aspiranteService.obtenerPorId("a1")).thenReturn(mock(AspiranteResponseDTO.class));

        mvc(controller)
                .perform(get("/api/v1/aspirantes/a1").with(autenticado("juanmod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerPorEmail_devuelve200() throws Exception {
        when(aspiranteService.obtenerPorEmail("juan.romero@example.com"))
                .thenReturn(mock(AspiranteResponseDTO.class));

        mvc(controller)
                .perform(get("/api/v1/aspirantes/email/juan.romero@example.com")
                        .with(autenticado("juanmod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerPorCodigo_publico_devuelve200() throws Exception {
        when(aspiranteService.obtenerPorCodigo("AB-123")).thenReturn(mock(AspiranteResponseDTO.class));

        mvc(controller)
                .perform(get("/api/v1/aspirantes/codigo/AB-123"))
                .andExpect(status().isOk());
    }

    @Test
    void listarTodos_devuelve200() throws Exception {
        when(aspiranteService.obtenerTodos()).thenReturn(java.util.List.of());

        mvc(controller)
                .perform(get("/api/v1/aspirantes").with(autenticado("juanmod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerPorEstado_devuelve200() throws Exception {
        when(aspiranteService.obtenerPorEstado(EstadoAspirante.PENDIENTE)).thenReturn(java.util.List.of());

        mvc(controller)
                .perform(get("/api/v1/aspirantes/estado/PENDIENTE").with(autenticado("juanmod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerEstadisticas_devuelve200() throws Exception {
        when(aspiranteService.obtenerEstadisticas()).thenReturn(mock(EstadisticasAspiranteDTO.class));

        mvc(controller)
                .perform(get("/api/v1/aspirantes/estadisticas").with(autenticado("juanmod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void aprobar_devuelve200() throws Exception {
        when(aspiranteService.aprobarAspirante("a1", "juanmod")).thenReturn(mock(AspiranteResponseDTO.class));

        mvc(controller)
                .perform(put("/api/v1/aspirantes/a1/aprobar").with(autenticado("juanmod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void rechazar_devuelve200() throws Exception {
        when(aspiranteService.rechazarAspirante(eq("a1"), eq("Documento ilegible"), eq("juanmod")))
                .thenReturn(mock(AspiranteResponseDTO.class));

        mvc(controller)
                .perform(json(put("/api/v1/aspirantes/a1/rechazar"),
                        "{\"motivo\":\"Documento ilegible\"}").with(autenticado("juanmod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void rechazar_sinMotivo_devuelve400() throws Exception {
        mvc(controller)
                .perform(json(put("/api/v1/aspirantes/a1/rechazar"), "{}").with(autenticado("juanmod", "MOD")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void solicitarCorreccion_devuelve200() throws Exception {
        when(aspiranteService.solicitarCorreccion(eq("a1"), anyString(), eq("juanmod")))
                .thenReturn(mock(AspiranteResponseDTO.class));

        mvc(controller)
                .perform(json(put("/api/v1/aspirantes/a1/solicitar-correccion"),
                        "{\"motivo\":\"Corrija el NIT\"}").with(autenticado("juanmod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void reenviarDocumentos_publico_devuelve200() throws Exception {
        when(aspiranteService.reenviarDocumentos(eq("AB-123"), any()))
                .thenReturn(mock(AspiranteResponseDTO.class));

        mvc(controller)
                .perform(json(put("/api/v1/aspirantes/codigo/AB-123/reenviar"),
                        "{\"rutaArchivo\":\"/uploads/nuevo.pdf\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void agregarComentario_devuelve200() throws Exception {
        when(aspiranteService.agregarComentarioInterno(eq("a1"), eq("Llamé al aspirante"), eq("juanmod")))
                .thenReturn(mock(AspiranteResponseDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/aspirantes/a1/comentarios"),
                        "{\"texto\":\"Llamé al aspirante\"}").with(autenticado("juanmod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void enviarCredenciales_devuelve200() throws Exception {
        when(aspiranteService.enviarCredenciales("a1", "juanmod"))
                .thenReturn(mock(AspiranteResponseDTO.class));

        mvc(controller)
                .perform(put("/api/v1/aspirantes/a1/enviar-credenciales")
                        .with(autenticado("juanmod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void actualizarEstado_devuelve200() throws Exception {
        when(aspiranteService.actualizarEstado("a1", EstadoAspirante.APROBADO))
                .thenReturn(mock(AspiranteResponseDTO.class));

        mvc(controller)
                .perform(put("/api/v1/aspirantes/a1/estado")
                        .param("estado", "APROBADO")
                        .with(autenticado("admin", "ADMIN")))
                .andExpect(status().isOk());
    }
}
package com.photobogota.api.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.photobogota.api.service.ImagenService;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImagenControllerTest extends ControllerTestSupport {

    private final ImagenService imagenService = mock(ImagenService.class);
    private final ImagenController controller = new ImagenController(imagenService);

    private MockMultipartFile imagen(String name) {
        return new MockMultipartFile("file", name, "image/png", new byte[]{1, 2, 3});
    }

    @Test
    void subirAvatar_devuelve200ConUrl() throws Exception {
        when(imagenService.subirAvatar(org.mockito.ArgumentMatchers.any()))
                .thenReturn("/uploads/avatars/foto.png");

        mvc(controller)
                .perform(multipart("/api/v1/imagenes/avatar").file(imagen("foto.png"))
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/uploads/avatars/foto.png"));
    }

    @Test
    void subirImagenSpot_devuelve200() throws Exception {
        when(imagenService.subirImagenSpot(org.mockito.ArgumentMatchers.any()))
                .thenReturn("/uploads/spots/foto.png");

        mvc(controller)
                .perform(multipart("/api/v1/imagenes/spot").file(imagen("foto.png"))
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }

    @Test
    void subirEvidenciaReporte_devuelve200() throws Exception {
        when(imagenService.subirEvidenciaReporte(org.mockito.ArgumentMatchers.any()))
                .thenReturn("/uploads/reportes/ev1.png");

        mvc(controller)
                .perform(multipart("/api/v1/imagenes/reporte").file(imagen("ev1.png"))
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }

    @Test
    void subirDocumentoAspirante_publico_devuelve200() throws Exception {
        when(imagenService.subirDocumentoAspirante(org.mockito.ArgumentMatchers.any()))
                .thenReturn("/uploads/aspirantes/cedula.pdf");

        MockMultipartFile pdf = new MockMultipartFile("file", "cedula.pdf",
                "application/pdf", new byte[]{37, 80, 68, 70});
        mvc(controller)
                .perform(multipart("/api/v1/imagenes/aspirante-documento").file(pdf))
                .andExpect(status().isOk());
    }

    @Test
    void subirImagen_conArchivoVacio_devuelve500() throws Exception {
        MockMultipartFile vacio = new MockMultipartFile("file", "vacio.png", "image/png", new byte[0]);

        mvc(controller)
                .perform(multipart("/api/v1/imagenes/avatar").file(vacio)
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void subirImagen_conTipoNoImagen_devuelve500() throws Exception {
        MockMultipartFile txt = new MockMultipartFile("file", "doc.txt", "text/plain", "hola".getBytes());

        mvc(controller)
                .perform(multipart("/api/v1/imagenes/avatar").file(txt)
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void subirAvatar_verificaQueDelegueEnElServicio() throws Exception {
        when(imagenService.subirAvatar(org.mockito.ArgumentMatchers.any()))
                .thenReturn("/uploads/avatars/a.png");

        mvc(controller)
                .perform(multipart("/api/v1/imagenes/avatar").file(imagen("a.png"))
                        .with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());

        verify(imagenService).subirAvatar(org.mockito.ArgumentMatchers.any());
    }
}
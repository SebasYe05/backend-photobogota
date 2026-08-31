package com.photobogota.api.storage;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudinaryStorageServiceTest {

    private Cloudinary cloudinary;
    private Uploader uploader;
    private CloudinaryStorageService cloudinaryStorageService;

    @BeforeEach
    void prepararServicio() {
        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);

        cloudinaryStorageService = new CloudinaryStorageService("cloudinary://key:secret@res.cloudinary.com/demo");
        ReflectionTestUtils.setField(cloudinaryStorageService, "cloudinary", cloudinary);
    }

    private MultipartFile archivoDeEjemplo() {
        return new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
    }

    @Test
    void guardar_devuelveElSecureUrl() throws IOException {
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/v1/carpeta/foto.webp"));

        String resultado = cloudinaryStorageService.guardar(archivoDeEjemplo(), "spots");

        assertThat(resultado).isEqualTo("https://res.cloudinary.com/demo/image/upload/v1/carpeta/foto.webp");
    }

    @Test
    void guardar_sinSecureUrl_usaLaUrlPlana() throws IOException {
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("url", "http://res.cloudinary.com/fallback/foto.jpg"));

        String resultado = cloudinaryStorageService.guardar(archivoDeEjemplo(), "spots");

        assertThat(resultado).isEqualTo("http://res.cloudinary.com/fallback/foto.jpg");
    }

    @Test
    void guardar_conErrorIo_lanzaRuntimeException() throws IOException {
        when(uploader.upload(any(byte[].class), any(Map.class))).thenThrow(new IOException("sin conexión"));

        assertThatThrownBy(() -> cloudinaryStorageService.guardar(archivoDeEjemplo(), "spots"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al subir archivo a Cloudinary");
    }

    @Test
    void eliminar_extraeElPublicIdSinVersion() throws IOException {
        cloudinaryStorageService.eliminar(
                "https://res.cloudinary.com/demo/image/upload/photobogota/spot/abc123");

        verify(uploader).destroy(eq("photobogota/spot/abc123"), anyMap());
    }

    @Test
    void eliminar_extraeElPublicIdConVersionYExtension() throws IOException {
        cloudinaryStorageService.eliminar(
                "https://res.cloudinary.com/demo/image/upload/v1620000000/photobogota/spot/abc123.jpg");

        verify(uploader).destroy(eq("photobogota/spot/abc123"), anyMap());
    }

    @Test
    void eliminar_sinUploadEnLaUrl_noLlamaDestroy() throws IOException {
        cloudinaryStorageService.eliminar("http://localhost:8080/uploads/carpeta/foto.jpg");

        verify(uploader, never()).destroy(any(String.class), anyMap());
    }

    @Test
    void eliminar_conErrorIo_lanzaRuntimeException() throws IOException {
        doThrow(new IOException("no se pudo destruir"))
                .when(uploader).destroy(eq("photobogota/spot/abc123"), anyMap());

        assertThatThrownBy(() -> cloudinaryStorageService.eliminar(
                "https://res.cloudinary.com/demo/image/upload/photobogota/spot/abc123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al eliminar archivo de Cloudinary");
    }
}
package com.photobogota.api.service;

import com.photobogota.api.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImagenServiceTest {

    @Mock
    private StorageService storageService;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private ImagenService imagenService;

    @Test
    void subirAvatar_guardaEnCarpetaAvatars() {
        when(storageService.guardar(file, "avatars")).thenReturn("/avatars/avatar.png");

        String url = imagenService.subirAvatar(file);

        assertThat(url).isEqualTo("/avatars/avatar.png");
        verify(storageService).guardar(file, "avatars");
    }

    @Test
    void subirImagenSpot_guardaEnCarpetaSpots() {
        when(storageService.guardar(file, "spots")).thenReturn("/spots/spot.png");

        String url = imagenService.subirImagenSpot(file);

        assertThat(url).isEqualTo("/spots/spot.png");
        verify(storageService).guardar(file, "spots");
    }

    @Test
    void subirEvidenciaReporte_guardaEnCarpetaReportes() {
        when(storageService.guardar(file, "reportes")).thenReturn("/reportes/evidencia.png");

        String url = imagenService.subirEvidenciaReporte(file);

        assertThat(url).isEqualTo("/reportes/evidencia.png");
        verify(storageService).guardar(file, "reportes");
    }

    @Test
    void subirDocumentoAspirante_guardaEnCarpetaAspirantes() {
        when(storageService.guardar(file, "aspirantes")).thenReturn("/aspirantes/documento.pdf");

        String url = imagenService.subirDocumentoAspirante(file);

        assertThat(url).isEqualTo("/aspirantes/documento.pdf");
        verify(storageService).guardar(file, "aspirantes");
    }

    @Test
    void cadaMetodo_usaSuCarpetaCorrespondiente() {
        ArgumentCaptor<String> carpetaCaptor = ArgumentCaptor.forClass(String.class);
        when(storageService.guardar(any(MultipartFile.class), anyString())).thenReturn("/carpeta/archivo");

        imagenService.subirAvatar(file);
        imagenService.subirImagenSpot(file);
        imagenService.subirEvidenciaReporte(file);
        imagenService.subirDocumentoAspirante(file);

        verify(storageService, times(4)).guardar(any(MultipartFile.class), carpetaCaptor.capture());
        assertThat(carpetaCaptor.getAllValues())
                .containsExactly("avatars", "spots", "reportes", "aspirantes");
    }
}
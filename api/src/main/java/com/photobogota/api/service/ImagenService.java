package com.photobogota.api.service;

import com.photobogota.api.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImagenService {

    private final StorageService storageService;

    public String subirAvatar(MultipartFile file) {
        return storageService.guardar(file, "avatars");
    }

    public String subirImagenSpot(MultipartFile file) {
        return storageService.guardar(file, "spots");
    }

    public String subirEvidenciaReporte(MultipartFile file) {
        return storageService.guardar(file, "reportes");
    }

    public String subirDocumentoAspirante(MultipartFile file) {
        return storageService.guardar(file, "aspirantes");
    }
}

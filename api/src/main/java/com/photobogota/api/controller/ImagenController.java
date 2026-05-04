package com.photobogota.api.controller;

import com.photobogota.api.service.ImagenService;
import com.photobogota.api.utils.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.V1 + "/imagenes")
@RequiredArgsConstructor
@Tag(name = "Imágenes", description = "Subida de imágenes al servidor")
public class ImagenController {

    private final ImagenService imagenService;

    @Operation(summary = "Subir avatar", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> subirAvatar(
            @RequestParam("file") MultipartFile file) throws IOException {
        validarImagen(file);
        String url = imagenService.subirAvatar(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @Operation(summary = "Subir imagen de spot", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/spot", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MIEMBRO', 'SOCIO', 'MODERADOR')")
    public ResponseEntity<Map<String, String>> subirImagenSpot(
            @RequestParam("file") MultipartFile file) throws IOException {
        validarImagen(file);
        String url = imagenService.subirImagenSpot(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    private void validarImagen(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("El archivo está vacío");
        if (file.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException("El archivo supera los 5MB");
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) throw new IllegalArgumentException("Solo se permiten imágenes");
    }
}
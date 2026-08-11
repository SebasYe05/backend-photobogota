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
            @RequestParam("file") MultipartFile file) {
        validarImagen(file);
        String url = imagenService.subirAvatar(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @Operation(summary = "Subir imagen de spot", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/spot", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MIEMBRO', 'SOCIO', 'MOD')")
    public ResponseEntity<Map<String, String>> subirImagenSpot(
            @RequestParam("file") MultipartFile file) {
        validarImagen(file);
        String url = imagenService.subirImagenSpot(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @Operation(summary = "Subir evidencia de un reporte", description = "Sube una captura de pantalla como evidencia. La URL devuelta se envía luego en 'evidencias' al crear el reporte.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/reporte", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> subirEvidenciaReporte(
            @RequestParam("file") MultipartFile file) {
        validarImagen(file);
        String url = imagenService.subirEvidenciaReporte(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @Operation(summary = "Subir documento de aspirante a socio", description = "Sube el RUT/cédula (PDF o imagen) de un aspirante. No requiere cuenta, ya que el aspirante aún no tiene una.")
    @PostMapping(value = "/aspirante-documento", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> subirDocumentoAspirante(
            @RequestParam("file") MultipartFile file) {
        validarDocumento(file);
        String url = imagenService.subirDocumentoAspirante(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    private void validarImagen(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("El archivo está vacío");
        if (file.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException("El archivo supera los 5MB");
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) throw new IllegalArgumentException("Solo se permiten imágenes");
    }

    private void validarDocumento(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("El archivo está vacío");
        if (file.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException("El archivo supera los 5MB");
        String ct = file.getContentType();
        boolean esValido = ct != null && (ct.startsWith("image/") || ct.equals("application/pdf"));
        if (!esValido) throw new IllegalArgumentException("Solo se permiten archivos PDF o imágenes (JPG, PNG)");
    }
}
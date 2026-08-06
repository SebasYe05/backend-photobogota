package com.photobogota.api.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Almacena los archivos en Cloudinary (plan gratuito).
 * Se activa solo si la propiedad {@code cloudinary.url} (variable CLOUDINARY_URL) está definida;
 * si no, entra en juego {@link LocalStorageService} como respaldo.
 * URL devuelta: https://res.cloudinary.com/<cloud_name>/<tipo>/upload/v<version>/<public_id>.<ext>
 */
@Service
@ConditionalOnProperty(name = "cloudinary.url")
public class CloudinaryStorageService implements StorageService {

    private final Cloudinary cloudinary;

    public CloudinaryStorageService(@Value("${cloudinary.url}") String cloudinaryUrl) {
        this.cloudinary = new Cloudinary(cloudinaryUrl);
        this.cloudinary.config.secure = true;
    }

    @Override
    public String guardar(MultipartFile archivo, String carpeta) {
        try {
            Map<?, ?> resultado = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "photobogota/" + carpeta,
                            "resource_type", "auto"
                    ));
            Object url = resultado.get("secure_url");
            if (url == null) url = resultado.get("url");
            return String.valueOf(url);
        } catch (IOException e) {
            throw new RuntimeException("Error al subir archivo a Cloudinary", e);
        }
    }

    @Override
    public void eliminar(String urlArchivo) {
        try {
            String publicId = extraerPublicId(urlArchivo);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar archivo de Cloudinary", e);
        }
    }

    /**
     * Extrae el public_id de una URL de Cloudinary:
     * ".../image/upload/v1234/photobogota/spots/abc123.jpg" -> "photobogota/spots/abc123"
     */
    private String extraerPublicId(String urlArchivo) {
        if (urlArchivo == null || !urlArchivo.contains("/upload/")) return null;
        String resto = urlArchivo.substring(urlArchivo.indexOf("/upload/") + "/upload/".length());
        if (resto.matches("v\\d+/.*")) {
            resto = resto.substring(resto.indexOf('/') + 1);
        }
        int ultimoPunto = resto.lastIndexOf('.');
        if (ultimoPunto > resto.lastIndexOf('/')) {
            resto = resto.substring(0, ultimoPunto);
        }
        return resto;
    }
}

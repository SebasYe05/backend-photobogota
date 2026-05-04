package com.photobogota.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ImagenService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String subirAvatar(MultipartFile file) throws IOException {
        return guardarArchivo(file, "avatars");
    }

    public String subirImagenSpot(MultipartFile file) throws IOException {
        return guardarArchivo(file, "spots");
    }

    private String guardarArchivo(MultipartFile file, String subcarpeta) throws IOException {
        // Ruta absoluta basada en el directorio de ejecución del proyecto
        Path carpeta = Paths.get(System.getProperty("user.dir"), uploadDir, subcarpeta);
        Files.createDirectories(carpeta);

        String extension = obtenerExtension(file.getOriginalFilename());
        String nombreArchivo = UUID.randomUUID() + extension;
        Path destino = carpeta.resolve(nombreArchivo);

        // Files.copy desde el InputStream evita el problema de transferTo con Tomcat
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destino, StandardCopyOption.REPLACE_EXISTING);
        }

        return "/uploads/" + subcarpeta + "/" + nombreArchivo;
    }

    private String obtenerExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) return ".jpg";
        return nombreOriginal.substring(nombreOriginal.lastIndexOf(".")).toLowerCase();
    }
}
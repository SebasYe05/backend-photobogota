package com.photobogota.api.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@Profile("local") // activar solo cuando spring.profiles.active=local
public class LocalStorageService implements StorageService {

    @Value("${storage.local.ruta:/uploads}")
    private String rutaBase;

    @Value("${storage.local.url-base:http://localhost:8080/uploads}")
    private String urlBase;

    @Override
    public String guardar(MultipartFile archivo, String carpeta) {
        try {
            String nombreUnico = UUID.randomUUID() + "_" + archivo.getOriginalFilename();
            Path destino = Paths.get(rutaBase, carpeta, nombreUnico);
            Files.createDirectories(destino.getParent());
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            return urlBase + "/" + carpeta + "/" + nombreUnico;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar imagen localmente", e);
        }
    }

    @Override
    public void eliminar(String urlArchivo) {
        try {
            // Convierte URL a ruta local: quita el prefijo urlBase
            String rutaRelativa = urlArchivo.replace(urlBase + "/", "");
            Path archivo = Paths.get(rutaBase, rutaRelativa);
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar imagen local", e);
        }
    }
}
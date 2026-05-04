package com.photobogota.api.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Guarda el archivo y retorna la URL pública para accederlo.
     * En local: http://localhost:8080/uploads/uuid_nombre.jpg
     * En S3: https://bucket.s3.amazonaws.com/spots/uuid_nombre.jpg
     */
    String guardar(MultipartFile archivo, String carpeta);

    /**
     * Elimina un archivo dado su URL pública (útil al borrar un spot).
     */
    void eliminar(String urlArchivo);
}

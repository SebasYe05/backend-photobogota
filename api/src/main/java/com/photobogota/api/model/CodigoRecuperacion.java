package com.photobogota.api.model;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modelo para almacenar códigos de recuperación de contraseña.
 * Cada código tiene una fecha de expiración y se elimina automáticamente
 * después de ser usado o expirado.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "codigos_recuperacion")
public class CodigoRecuperacion {
    
    @Id
    private ObjectId id;

    @Indexed
    private String email;

    private String codigo;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaExpiracion;

    private boolean usado;

    /**
     * Verifica si el código ha expirado.
     * 
     * @return true si la fecha de expiración es anterior a la hora actual
     */
    public boolean estaExpirado() {
        return LocalDateTime.now().isAfter(fechaExpiracion);
    }

    /**
     * Verifica si el código es válido (no usado y no expirado).
     * 
     * @return true si el código es válido
     */
    public boolean esValido() {
        return !usado && !estaExpirado();
    }
}
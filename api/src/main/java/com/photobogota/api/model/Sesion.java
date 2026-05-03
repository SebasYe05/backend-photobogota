package com.photobogota.api.model;

import lombok.Builder;
import lombok.Data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "sesiones")
@Builder
public class Sesion {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private String refreshToken;

    private String usuarioId; 
    private String nombreUsuario;
    private String rol; 

    private Instant creadoEn;
    private Instant expiraEn;
    private boolean activo;

    private String userAgent;
}
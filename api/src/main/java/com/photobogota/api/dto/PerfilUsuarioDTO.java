package com.photobogota.api.dto;

import org.bson.types.ObjectId;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PerfilUsuarioDTO {
    private ObjectId id;
    private String nombreUsuario;
    private String nombresCompletos;
    private String email;
    private String telefono;
    private String fotoPefil;
    private String biografia;
    
    private String tipoUsuario;

    @Builder.Default
    private Long puntos = 0L; 

    @Builder.Default 
    private Integer nivel = 1;
}
package com.photobogota.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO para la solicitud de edición de perfil de usuario.
 */
@Data
@Builder
public class EditarPerfilDTO {
    
    private String nombresCompletos;
    
    private String telefono;
    
    private String fotoPerfil;
    
    private String biografia;
}
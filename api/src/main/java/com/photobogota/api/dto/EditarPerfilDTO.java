package com.photobogota.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de edición de perfil de usuario.
 */
@Data
@Builder
@NoArgsConstructor  
@AllArgsConstructor 
public class EditarPerfilDTO {
    
    private String nombresCompletos;
    
    private String telefono;
    
    private String fotoPerfil;
    
    private String biografia;
}
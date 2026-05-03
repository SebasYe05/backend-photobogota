package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor  
@AllArgsConstructor 
@Schema(description = "DTO para editar el perfil del usuario")
public class EditarPerfilDTO {
    
    @Schema(description = "Nombres completos del usuario", example = "Juan Sebastian Sotomayor")
    private String nombresCompletos;
    
    @Schema(description = "Número de teléfono del usuario", example = "3001234567")
    private String telefono;
    
    @Schema(description = "URL de la foto de perfil del usuario", example = "/perfiles/foto_perfil.jpg")
    private String fotoPerfil;
    
    @Schema(description = "Biografía del usuario", example = "Parchado, amante de la fotografía y la naturaleza.")
    private String biografia;
}
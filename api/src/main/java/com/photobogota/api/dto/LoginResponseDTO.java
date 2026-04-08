package com.photobogota.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta de autenticación exitosa.
 * Contiene el token JWT y información del usuario.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {
    
    /**
     * Token JWT de acceso
     */
    private String token;
    
    /**
     * Token de refresh para renovar la sesión
     */
    private String refreshToken;
    
    /**
     * Nombre de usuario
     */
    private String nombreUsuario;
    
    /**
     * Email del usuario
     */
    private String email;
    
    /**
     * Rol del usuario
     */
    private String rol;
    
    /**
     * Nivel del usuario (para miembros)
     */
    private Integer nivel;
    
    private String nombresCompletos;  
    private String telefono;          
    private String biografia;          
    private String fotoPerfil;   

    /**
     * Mensaje de éxito
     */
    private String mensaje;
}

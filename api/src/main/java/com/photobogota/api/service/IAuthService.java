package com.photobogota.api.service;

import com.photobogota.api.dto.LoginRequestDTO;
import com.photobogota.api.dto.LoginResponseDTO;

/**
 * Interfaz para el servicio de autenticación.
 * Define los métodos necesarios para el login de usuarios.
 */
public interface IAuthService {
    
    /**
     * Autentica a un usuario con sus credenciales.
     * 
     * @param request DTO con las credenciales del usuario
     * @return DTO con el token JWT y información del usuario
     */
    LoginResponseDTO login(LoginRequestDTO request);
}

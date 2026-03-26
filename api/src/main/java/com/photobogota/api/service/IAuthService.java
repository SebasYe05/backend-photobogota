package com.photobogota.api.service;

import com.photobogota.api.dto.LoginRequestDTO;
import com.photobogota.api.dto.LoginResponseDTO;
import com.photobogota.api.dto.LogoutResponseDTO;
import com.photobogota.api.dto.RegistroRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;

/**
 * Interfaz para el servicio de autenticación.
 * Define los métodos necesarios para el login de usuarios.
 */
public interface IAuthService {
    
    /**
     * Registra un nuevo usuario en el sistema.
     * 
     * @param dto DTO con los datos del usuario a registrar
     * @return DTO con la respuesta del registro
     */
    RegistroResponseDTO registrar(RegistroRequestDTO dto);

     /**
     * Autentica a un usuario con sus credenciales.
     * 
     * @param request DTO con las credenciales del usuario
     * @return DTO con el token JWT y información del usuario
     */
    LoginResponseDTO login(LoginRequestDTO request);

    /**
     * Refresca el token JWT usando un refresh token.
     * 
     * @param refreshToken El token de refresh
     * @return DTO con el nuevo token JWT y refresh token
     */
    LoginResponseDTO refreshToken(String refreshToken);

    /**
     * Cierra la sesión del usuario revocando el refresh token.
     * 
     * @param refreshToken El token de refresh a revocar
     * @return DTO con el mensaje de confirmación
     */
    LogoutResponseDTO logout(String refreshToken);
}

package com.photobogota.api.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.photobogota.api.config.JwtService;
import com.photobogota.api.dto.LoginRequestDTO;
import com.photobogota.api.dto.LoginResponseDTO;
import com.photobogota.api.exception.InvalidCredentialsException;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.UsuarioAuthRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación del servicio de autenticación.
 * Maneja el login de usuarios generando tokens JWT.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final JwtService jwtService;
    private final IRefreshToken refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Autentica a un usuario con sus credenciales.
     * Busca el usuario por email o nombre de usuario, verifica la contraseña y
     * genera los tokens.
     * 
     * @param request DTO con las credenciales del usuario (login y contraseña)
     * @return LoginResponseDTO con el token JWT, refresh token e información del
     *         usuario
     * @throws InvalidCredentialsException si las credenciales son inválidas
     */
    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        // 1. Buscar al usuario por email O por nombre de usuario
        UsuarioAuth usuario = usuarioAuthRepository.findByEmailOrNombreUsuario(request.getLogin(), request.getLogin())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales inválidas"));

        // 2. Verificar si la contraseña coincide
        if (!passwordEncoder.matches(request.getContrasena(), usuario.getContrasena())) {
            throw new InvalidCredentialsException("Credenciales inválidas");
        }

        // 3. Generar el token JWT con claims adicionales (rol y email)
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("rol", usuario.getRol().name());
        extraClaims.put("email", usuario.getEmail());
        
        String token = jwtService.generarToken(extraClaims, usuario.getNombreUsuario());

        // 4. Generar el refresh token
        String refreshToken = refreshTokenService.crearRefreshToken(usuario.getEmail());

        // 5. Mapear y retornar la respuesta
        return LoginResponseDTO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .email(usuario.getEmail())
                .nombreUsuario(usuario.getNombreUsuario())
                .rol(usuario.getRol().name())
                .mensaje("Autenticación exitosa")
                .build();
    }

}

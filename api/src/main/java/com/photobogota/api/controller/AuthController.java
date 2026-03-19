package com.photobogota.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.LoginRequestDTO;
import com.photobogota.api.dto.LoginResponseDTO;
import com.photobogota.api.service.IAuthService;

import lombok.RequiredArgsConstructor;

/**
 * Controlador para manejar las operaciones de autenticación.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final IAuthService authService;

    /**
     * Endpoint para iniciar sesión.
     * Autentica al usuario y retorna los tokens JWT.
     * 
     * @param request DTO con las credenciales del usuario
     * @return LoginResponseDTO con el token JWT y datos del usuario
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}

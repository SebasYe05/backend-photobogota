package com.photobogota.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.LoginRequestDTO;
import com.photobogota.api.dto.LoginResponseDTO;
import com.photobogota.api.dto.RefreshTokenRequestDTO;
import com.photobogota.api.dto.RegistroRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;
import com.photobogota.api.service.IAuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador para manejar las operaciones de autenticación.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegistroResponseDTO> registrar(@Valid @RequestBody RegistroRequestDTO dto) {
        // Al usar @Valid, Spring revisa las anotaciones (@Email, @NotBlank) del DTO
        RegistroResponseDTO nuevoUsuario = authService.registrar(dto);
        return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }

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

    /**
     * Endpoint para actualizar el token JWT usando un refresh token.
     * 
     * @param request DTO con el refresh token
     * @return LoginResponseDTO con el nuevo token JWT y refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refreshToken(@RequestBody RefreshTokenRequestDTO request) {
        LoginResponseDTO response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
}

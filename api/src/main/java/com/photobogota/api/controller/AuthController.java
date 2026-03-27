package com.photobogota.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.LoginRequestDTO;
import com.photobogota.api.dto.LoginResponseDTO;
import com.photobogota.api.dto.LogoutResponseDTO;
import com.photobogota.api.dto.RefreshTokenRequestDTO;
import com.photobogota.api.dto.RegistroRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;
import com.photobogota.api.service.IAuthService;
import com.photobogota.api.dto.UsuarioResumenDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador para manejar las operaciones de autenticación.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "http://localhost:5173") // Permitir CORS para el frontend en desarrollo
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
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
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

    /**
     * Endpoint para cerrar sesión.
     * Revoca el refresh token para invalidar la sesión del usuario.
     * 
     * @param request DTO con el refresh token
     * @return LogoutResponseDTO con el mensaje de confirmación
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponseDTO> logout(@RequestBody(required = false) RefreshTokenRequestDTO request) {
        // Si no hay token en el cuerpo, igual devolvemos OK para que el front limpie su
        // estado
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isEmpty()) {
            return ResponseEntity.ok(LogoutResponseDTO.builder()
                    .mensaje("Sesión cerrada localmente")
                    .build());
        }

        LogoutResponseDTO response = authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /** 
     * Endpoint para obtener un resumen de los datos del usuario autenticado.
     * Usa @AuthenticationPrincipal para acceder al usuario autenticado desde el token JWT.
     * 
     * @return UsuarioResumenDTO con los datos básicos del usuario (nombre, foto, rol)
     * @throws InvalidCredentialsException Si el usuario no se encuentra
     */
    @GetMapping("/me")
    public ResponseEntity<UsuarioResumenDTO> obtenerUsuarioAutenticado(
            @AuthenticationPrincipal UserDetails currentUser) {

        // El service busca los datos básicos (nombre, foto, rol) usando el username del
        // token
        UsuarioResumenDTO user = authService.getResumenUsuario(currentUser.getUsername());
        return ResponseEntity.ok(user);
    }

}

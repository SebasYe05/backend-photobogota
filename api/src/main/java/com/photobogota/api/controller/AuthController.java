package com.photobogota.api.controller;

import java.util.Map;

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
import com.photobogota.api.dto.SolicitarRecuperacionDTO;
import com.photobogota.api.dto.UsuarioResumenDTO;
import com.photobogota.api.dto.VerificarCodigoDTO;
import com.photobogota.api.service.IAuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.photobogota.api.utils.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping(ApiConstants.V1 + "/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Autenticación", description = "Endpoints para registro, login, logout y gestión de sesión")
public class AuthController {

    private final IAuthService authService;

    @Operation(summary = "Registrar nuevo usuario", description = "Crea una cuenta nueva con rol MIEMBRO por defecto")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o email/usuario ya registrado")
    })
    @PostMapping("/register")
    public ResponseEntity<RegistroResponseDTO> registrar(@Valid @RequestBody RegistroRequestDTO dto) {
        RegistroResponseDTO nuevoUsuario = authService.registrar(dto);
        return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }

    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario con email o nombre de usuario y retorna los tokens JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso, retorna access token y refresh token"),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas"),
            @ApiResponse(responseCode = "403", description = "Cuenta desactivada")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Actualizar token JWT", description = "Actualiza el token JWT usando un refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token actualizado exitosamente"),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido o expirado")
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refreshToken(@RequestBody RefreshTokenRequestDTO request) {
        LoginResponseDTO response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cerrar sesión", description = "Revoca el refresh token para invalidar la sesión del usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión cerrada exitosamente"),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido o expirado")
    })
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponseDTO> logout(@RequestBody(required = false) RefreshTokenRequestDTO request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isEmpty()) {
            return ResponseEntity.ok(LogoutResponseDTO.builder()
                    .mensaje("Sesión cerrada localmente")
                    .build());
        }

        LogoutResponseDTO response = authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener usuario autenticado", description = "Retorna los datos del usuario dueño del token JWT activo", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos del usuario autenticado"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/me")
    public ResponseEntity<UsuarioResumenDTO> obtenerUsuarioAutenticado(
            @AuthenticationPrincipal UserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            UsuarioResumenDTO user = authService.getResumenUsuario(currentUser.getUsername());
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(summary = "Solicitar recuperación de contraseña", description = "Envía un código de 6 dígitos al email registrado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código enviado al email"),
            @ApiResponse(responseCode = "404", description = "Email no registrado")
    })
    @PostMapping("/passwords/recovery-request")
    public ResponseEntity<Map<String, String>> solicitarRecuperacion(
            @Valid @RequestBody SolicitarRecuperacionDTO dto) {
        String mensaje = authService.solicitarRecuperacionContrasena(dto);
        return ResponseEntity.ok(Map.of("mensaje", mensaje));
    }

    @Operation(summary = "Verificar código y cambiar contraseña", description = "Valida el código recibido por email y actualiza la contraseña")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada correctamente"),
            @ApiResponse(responseCode = "400", description = "Código inválido o expirado")
    })
    @PostMapping("/passwords/reset")
    public ResponseEntity<Map<String, String>> verificarCodigo(
            @Valid @RequestBody VerificarCodigoDTO dto) {
        String mensaje = authService.verificarCodigoYCambiarContrasena(dto);
        return ResponseEntity.ok(Map.of("mensaje", mensaje));
    }

    @Operation(summary = "Verificar sesión", description = "Verifica si el token JWT es válido y retorna información del usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión válida"),
            @ApiResponse(responseCode = "401", description = "Token inválido o expirado")
    })
    @GetMapping("/verify-session")
    public ResponseEntity<Map<String, Object>> verificarSesion(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        // Devuelve el rol que tiene el JWT firmado (fuente de verdad del backend)
        return ResponseEntity.ok(Map.of(
                "nombreUsuario", userDetails.getUsername(),
                "rol", userDetails.getAuthorities().iterator().next().getAuthority()
                        .replace("ROLE_", "")));
    }

}
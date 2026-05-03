package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO para la respuesta de inicio de sesión")
public class LoginResponseDTO {

    @Schema(description = "Token JWT de acceso", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Token de refresh para renovar la sesión")
    private String refreshToken;

    @Schema(description = "Nombre de usuario", example = "juan.romero")
    private String nombreUsuario;

    @Schema(description = "Email del usuario", example = "juan.romero@example.com")
    private String email;

    @Schema(description = "Rol del usuario", example = "MIEMBRO")
    private String rol;

    @Schema(description = "Nivel del usuario (para miembros)", example = "1")
    private Integer nivel;

    @Schema(description = "Nombres completos del usuario", example = "Juan Romero")
    private String nombresCompletos;

    @Schema(description = "Teléfono del usuario", example = "3001234567")
    private String telefono;

    @Schema(description = "Biografía del usuario", example = "Holi, me gusta alcorilicoz y la fotografía")
    private String biografia;

    @Schema(description = "URL de la foto de perfil", example = "https://example.com/foto-perfil.jpg")
    private String fotoPerfil;

    @Schema(description = "Mensaje de respuesta", example = "Inicio de sesión exitoso")
    private String mensaje;
}

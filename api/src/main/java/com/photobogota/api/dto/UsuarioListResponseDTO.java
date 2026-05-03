package com.photobogota.api.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO para la respuesta de la lista de usuarios")
public class UsuarioListResponseDTO {

    @Schema(description = "ID del usuario")
    private String id;

    @Schema(description = "Nombres completos del usuario", example = "Juan Sebastian Romero Ramirez")
    private String nombresCompletos;

    @Schema(description = "Correo electrónico del usuario", example = "juan.romero@example.com")
    private String email;

    @Schema(description = "Nombre de usuario", example = "juan_romero")
    private String nombreUsuario;

    @Schema(description = "Rol del usuario", example = "MIEMBRO")
    private String rol;

    @Schema(description = "Estado de la cuenta", example = "true")
    private Boolean estadoCuenta;

    @Schema(description = "Fecha de registro", example = "2026-01-01T00:00:00Z")
    private LocalDateTime fechaRegistro;
}

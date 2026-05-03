package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "DTO para el resumen de información del usuario")
public class UsuarioResumenDTO {
    @Schema(description = "Nombre de usuario", example = "juan_romero")
    private String nombreUsuario;

    @Schema(description = "Nombres completos del usuario", example = "Juan Sebastian Romero Ramirez")
    private String nombresCompletos;

    @Schema(description = "Foto de perfil del usuario", example = "/avatars/juan.jpg")
    private String fotoPerfil;

    @Schema(description = "Rol del usuario", example = "MIEMBRO")
    private String rol;

    @Schema(description = "Nivel del usuario", example = "1")
    private Integer nivel;

    @Schema(description = "Correo electrónico del usuario", example = "juan.romero@example.com")
    private String email;

    @Schema(description = "Biografía del usuario", example = "Holi, me gusta alcoliricoz y la fotografía")
    private String biografia;

    @Schema(description = "Número de teléfono del usuario", example = "3201234567")
    private String telefono;
}

package com.photobogota.api.dto;

import org.bson.types.ObjectId;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "DTO para el perfil del usuario")
public class PerfilUsuarioDTO {

    @Schema(description = "ID del usuario")
    private ObjectId id;

    @Schema(description = "Nombre de usuario", example = "juan.romero")
    private String nombreUsuario;

    @Schema(description = "Nombres completos", example = "Juan Romero")
    private String nombresCompletos;

    @Schema(description = "Email", example = "juan.romero@example.com")
    private String email;

    @Schema(description = "Teléfono", example = "3001234567")
    private String telefono;

    @Schema(description = "URL de la foto de perfil", example = "https://example.com/foto-perfil.jpg")
    private String fotoPerfil;

    @Schema(description = "Biografía del usuario", example = "Holi, me gusta alcorilicoz y la fotografía")
    private String biografia;

    @Schema(description = "Rol del usuario", example = "MIEMBRO")
    private String rol;

    @Schema(description = "Número de puntos acumulados", example = "150")
    @Builder.Default
    private Long puntos = 0L; 

    @Schema(description = "Nivel del usuario basado en los puntos acumulados (null si el usuario no es MIEMBRO)", example = "1")
    private Integer nivel;

    @Schema(description = "Total de spots publicados por el usuario", example = "5")
    @Builder.Default
    private Integer totalSpots = 0;

    @Schema(description = "Total de reseñas escritas por el usuario", example = "12")
    @Builder.Default
    private Integer totalResenas = 0;

    @Schema(description = "Total de spots guardados por el usuario", example = "3")
    @Builder.Default
    private Integer totalGuardados = 0;
}
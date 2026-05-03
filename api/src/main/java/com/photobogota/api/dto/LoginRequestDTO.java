package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO para la solicitud de inicio de sesión")
public class LoginRequestDTO {

    @NotBlank(message = "El usuario o email es requerido")
    @Schema(description = "Nombre de usuario o email", example = "juan.romero@example.com")
    private String login;

    @NotBlank(message = "La contraseña es requerida")
    @Schema(description = "Contraseña del usuario", example = "Segura123.")
    private String contrasena;

}

package com.photobogota.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitar autenticación de usuario.
 * Puede contener email o nombre de usuario.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "El usuario o email es requerido")
    private String login;

    /**
     * Contraseña del usuario
     */
    @NotBlank(message = "La contraseña es requerida")
    private String contrasena;
}

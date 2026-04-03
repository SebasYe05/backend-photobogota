package com.photobogota.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitar el envío de código de recuperación de contraseña.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SolicitarRecuperacionDTO {
    
    @NotBlank(message = "El email es requerido")
    @Email(message = "El formato del email no es válido")
    private String email;
}
package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO para verificar el código de recuperación de contraseña")
public class VerificarCodigoDTO {
    
    @NotBlank(message = "El email es requerido")
    @Email(message = "El formato del email no es válido")
    @Schema(description = "Correo electrónico del usuario", example = "juan.romero@example.com")
    private String email;
    
    @NotBlank(message = "El código es requerido")
    @Schema(description = "Código de recuperación de contraseña", example = "487568")
    private String codigo;
    
    @NotBlank(message = "La nueva contraseña es requerida")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    @Schema(description = "Nueva contraseña", example = "NuevaContrasena123.")
    private String nuevaContrasena;
}
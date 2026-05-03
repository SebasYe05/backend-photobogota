package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "DTO para la solicitud de cambio de contraseña")
public class CambiarContrasenaDTO {

    @NotBlank(message = "La contraseña actual es obligatoria")
    @Schema(description = "Contraseña actual del usuario", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contrasenaActual;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
    @Schema(description = "Nueva contraseña del usuario", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nuevaContrasena;

    @NotBlank(message = "La confirmación de la nueva contraseña es obligatoria")
    @Schema(description = "Confirmación de la nueva contraseña", requiredMode = Schema.RequiredMode.REQUIRED)
    private String confirmarContrasena;
}

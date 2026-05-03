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
@Schema(description = "DTO de respuesta para el cambio de contraseña")
public class CambiarContrasenaResponseDTO {

    @Schema(description = "Mensaje de confirmación", example = "Contraseña actualizada correctamente")
    private String mensaje;
}

package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta del logout.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO para la respuesta del logout.")
public class LogoutResponseDTO {

    @Schema(description = "Mensaje de respuesta", example = "Sesión cerrada exitosamente")
    private String mensaje;
}

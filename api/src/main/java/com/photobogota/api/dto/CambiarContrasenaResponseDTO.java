package com.photobogota.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta del cambio de contraseña.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CambiarContrasenaResponseDTO {

    private String mensaje;
}

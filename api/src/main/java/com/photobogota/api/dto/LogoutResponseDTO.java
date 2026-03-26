package com.photobogota.api.dto;

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
public class LogoutResponseDTO {

    private String mensaje;
}

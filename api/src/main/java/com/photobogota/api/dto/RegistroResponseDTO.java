package com.photobogota.api.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO de respuesta para el registro de un nuevo usuario")
public class RegistroResponseDTO {

    @Schema(description = "Fecha y hora del registro del nuevo usuario", example = "2024-06-01T12:00:00")
    private LocalDateTime fechaRegistro;

    @Schema(description = "Mensaje de confirmación del registro", example = "Usuario registrado exitosamente")
    private String mensaje;
}

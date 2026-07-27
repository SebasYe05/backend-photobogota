package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO para escalar un reporte de moderador a administrador")
public class EscalarReporteRequestDTO {

    @Size(max = 500, message = "El motivo no puede exceder los 500 caracteres")
    @Schema(description = "Motivo opcional por el que se escala el reporte", example = "Requiere revisión de un administrador por posible implicación legal")
    private String motivo;
}

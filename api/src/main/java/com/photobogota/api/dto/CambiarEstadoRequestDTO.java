package com.photobogota.api.dto;

import com.photobogota.api.model.EstadoReporte;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO para cambiar el estado de un reporte")
public class CambiarEstadoRequestDTO {

    @NotNull(message = "El estado es obligatorio")
    @Schema(description = "Nuevo estado del reporte", example = "EN_REVISION")
    private EstadoReporte estado;

    @Size(max = 500, message = "La observación no puede exceder los 500 caracteres")
    @Schema(description = "Observación opcional sobre el cambio de estado", example = "Se contactó al spot para corregir la información")
    private String observacion;
}

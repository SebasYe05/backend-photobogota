package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO para que un moderador apruebe o rechace una solución propuesta por un SOCIO/ADMIN")
public class ValidarReporteRequestDTO {

    @NotNull(message = "Debes indicar si apruebas o rechazas la solución")
    @Schema(description = "true = aprobado (se notifica al miembro), false = rechazado (vuelve a EN_REVISION)", example = "true")
    private Boolean aprobado;

    @Size(max = 500, message = "La observación no puede exceder los 500 caracteres")
    @Schema(description = "Observación opcional sobre la validación", example = "Solución verificada, se procede a notificar")
    private String observacion;
}
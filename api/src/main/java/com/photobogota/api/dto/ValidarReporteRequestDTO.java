package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO para que un MOD valide (o rechace) un reporte marcado como Solucionado por un SOCIO/ADMIN")
public class ValidarReporteRequestDTO {

    @NotNull(message = "Debes indicar si apruebas o rechazas la solución")
    @Schema(description = "true para aprobar la solución (pasa a RESUELTO y se notifica al miembro), false para rechazarla (vuelve a EN_REVISION)", example = "true")
    private Boolean aprobado;

    @Size(max = 500, message = "La observación no puede exceder los 500 caracteres")
    @Schema(description = "Observación opcional sobre la validación (obligatoria en la práctica si se rechaza)", example = "La descripción no explica qué se corrigió, por favor detallar")
    private String observacion;
}

package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Respuesta con el estado de puntos y nivel del usuario")
public class PuntosResponseDTO {

    @Schema(description = "Puntos acumulados totales", example = "150")
    private Long puntos;

    @Schema(description = "Nivel actual del usuario", example = "2")
    private Integer nivel;

    @Schema(description = "Puntos ganados hoy (resetea a medianoche)", example = "13")
    private Integer puntosHoy;

    @Schema(description = "Límite diario de puntos", example = "100")
    private Integer limiteDiario;

    @Schema(description = "Puntos restantes para subir al siguiente nivel", example = "50")
    private Long puntosParaSiguienteNivel;

    @Schema(description = "Porcentaje de progreso hacia el siguiente nivel (0-100)", example = "75")
    private Integer progresoPercent;
}

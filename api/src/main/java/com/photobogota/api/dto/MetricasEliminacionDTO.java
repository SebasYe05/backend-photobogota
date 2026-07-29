package com.photobogota.api.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Métricas agregadas sobre las solicitudes de eliminación de cuenta")
public class MetricasEliminacionDTO {

    private long totalSolicitudes;

    @Schema(description = "Cantidad de solicitudes agrupadas por estado")
    private Map<String, Long> porEstado;

    @Schema(description = "Cantidad de solicitudes agrupadas por motivo indicado")
    private Map<String, Long> porMotivo;

    @Schema(description = "Cantidad de solicitudes completadas agrupadas por rol del usuario")
    private Map<String, Long> porRol;

    @Schema(description = "Promedio de días entre la confirmación y la anonimización definitiva")
    private Double promedioDiasHastaCompletada;

    @Schema(description = "Solicitudes completadas en los últimos 30 días")
    private long completadasUltimos30Dias;

    @Schema(description = "Cantidad de solicitudes procesadas manualmente por un administrador (antes del plazo de 30 días)")
    private long procesadasManualmentePorAdmin;
}

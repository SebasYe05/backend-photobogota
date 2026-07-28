package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Dependencias del usuario detectadas automáticamente antes de procesar su eliminación")
public class DependenciasEliminacionDTO {

    @Schema(description = "Cantidad de spots creados por el usuario (aplica a SOCIO/MOD)")
    private int spotsCreados;

    @Schema(description = "Reportes que el propio usuario presentó y siguen pendientes (NUEVO o EN_REVISION)")
    private int reportesPendientesComoAutor;

    @Schema(description = "Reportes pendientes sobre spots o reseñas creados por el usuario")
    private int reportesPendientesSobreSuContenido;

    @Schema(description = "true si existe al menos una dependencia que el sistema resolverá automáticamente al procesar")
    private boolean tieneDependenciasPendientes;
}

package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estadísticas agregadas de las solicitudes de aspirantes a socio")
public class EstadisticasAspiranteDTO {

    @Schema(description = "Total de solicitudes registradas")
    private long total;

    @Schema(description = "Solicitudes pendientes de revisión")
    private long pendientes;

    @Schema(description = "Solicitudes devueltas para corrección")
    private long enCorreccion;

    @Schema(description = "Solicitudes aprobadas en espera de envío de credenciales")
    private long enEnvioCredenciales;

    @Schema(description = "Solicitudes aprobadas y con cuenta ya creada")
    private long aprobadas;

    @Schema(description = "Solicitudes rechazadas")
    private long rechazadas;

    @Schema(description = "Solicitudes procesadas (aprobadas + rechazadas), usadas para medir la carga resuelta")
    private long procesadas;
}

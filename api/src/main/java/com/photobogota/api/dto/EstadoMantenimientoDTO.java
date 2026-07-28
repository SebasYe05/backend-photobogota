package com.photobogota.api.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Estado actual de mantenimiento, consultado por el front (y por el propio
 * MantenimientoFilter) en GET /api/v1/mantenimiento/estado.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EstadoMantenimientoDTO {

    private boolean enMantenimiento;

    // Solo si enMantenimiento = true
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String mensaje;

    // Solo si enMantenimiento = false y hay una ventana futura programada
    // (útil para que el front muestre un aviso previo, ej. "banner")
    private LocalDateTime proximoInicio;
    private LocalDateTime proximoFin;
}

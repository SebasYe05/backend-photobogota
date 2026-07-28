package com.photobogota.api.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MantenimientoResponseDTO {

    private String id;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String mensaje;
    private String motivo;
    private String creadoPor;
    private Boolean cancelado;
    private LocalDateTime fechaCreacion;
}

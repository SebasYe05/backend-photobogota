package com.photobogota.api.dto;

import java.time.LocalDateTime;

import com.photobogota.api.model.MotivoEliminacionCuenta;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Estado actual de la solicitud de eliminación de cuenta del usuario autenticado")
public class EstadoEliminacionDTO {

    @Schema(description = "Indica si el usuario tiene una solicitud de eliminación activa")
    private boolean tieneSolicitudActiva;

    @Schema(description = "Estado de la solicitud: PENDIENTE_VERIFICACION, PROGRAMADA, CANCELADA o COMPLETADA")
    private String estado;

    private MotivoEliminacionCuenta motivo;

    private String comentario;

    private LocalDateTime fechaSolicitud;

    private LocalDateTime fechaConfirmacion;

    @Schema(description = "Fecha en la que la cuenta será anonimizada definitivamente si no se cancela antes")
    private LocalDateTime fechaProgramadaEliminacion;

    @Schema(description = "Días restantes para poder recuperar la cuenta, solo aplica si el estado es PROGRAMADA")
    private Long diasRestantes;
}

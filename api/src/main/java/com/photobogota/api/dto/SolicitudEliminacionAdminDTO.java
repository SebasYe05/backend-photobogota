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
@Schema(description = "Solicitud de eliminación de cuenta vista desde el panel de administración")
public class SolicitudEliminacionAdminDTO {

    private String id;

    private String usuarioId;

    private String nombreUsuario;

    private String email;

    @Schema(description = "Rol actual del usuario: MIEMBRO, SOCIO, MOD o ADMIN")
    private String rol;

    private MotivoEliminacionCuenta motivo;

    private String comentario;

    @Schema(description = "PENDIENTE_VERIFICACION, PROGRAMADA, CANCELADA o COMPLETADA")
    private String estado;

    private LocalDateTime fechaSolicitud;

    private LocalDateTime fechaConfirmacion;

    private LocalDateTime fechaProgramadaEliminacion;

    private LocalDateTime fechaCancelacion;

    private LocalDateTime fechaCompletada;

    private Long diasRestantes;

    @Schema(description = "true si la cuenta que hizo la solicitud coincide con los datos registrados (verificación automática de identidad)")
    private boolean identidadVerificada;

    private DependenciasEliminacionDTO dependencias;

    @Schema(description = "true si un administrador forzó el procesamiento en vez del job automático de 30 días")
    private boolean procesadaManualmente;

    private String procesadaPorAdmin;

    private String observacionAdmin;
}

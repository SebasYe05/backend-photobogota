package com.photobogota.api.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO de respuesta para un canje de promoción")
public class CanjeResponseDTO {

    @Schema(description = "ID del canje")
    private String id;

    @Schema(description = "Código de 8 caracteres alfanuméricos generado al canjear", example = "XK4P2M9Q")
    private String codigo;

    @Schema(description = "Estado derivado: VIGENTE, USADO o EXPIRADO", example = "VIGENTE")
    private String estado;

    @Schema(description = "Fecha en la que el miembro realizó el canje")
    private LocalDateTime fechaCanje;

    @Schema(description = "Fecha de expiración del canje (la de fin de la promoción)")
    private LocalDateTime fechaExpiracion;

    @Schema(description = "ID de la promoción canjeada")
    private String promocionId;

    @Schema(description = "Título de la promoción canjeada", example = "50% de descuento en sesión de fotos")
    private String promocionTitulo;

    @Schema(description = "Descuento de la promoción canjeada", example = "50%")
    private String descuento;

    @Schema(description = "ID del local donde se canjea")
    private String spotId;

    @Schema(description = "Nombre del local, denormalizado", example = "FotoEstudio Bogotá")
    private String spotNombre;

    @Schema(description = "nombreUsuario del miembro que canjeó")
    private String miembroNombre;
}
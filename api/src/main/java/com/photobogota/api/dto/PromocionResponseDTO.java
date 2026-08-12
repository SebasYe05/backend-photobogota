package com.photobogota.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO de respuesta para una promoción de un local")
public class PromocionResponseDTO {

    @Schema(description = "ID de la promoción")
    private String id;

    @Schema(description = "ID del local (spot) al que pertenece")
    private String spotId;

    @Schema(description = "Nombre del local, denormalizado", example = "FotoEstudio Bogotá")
    private String nombreSpot;

    @Schema(description = "nombreUsuario del socio dueño del local")
    private String socioUsername;

    @Schema(description = "Título de la promoción", example = "50% de descuento en sesión de fotos")
    private String titulo;

    @Schema(description = "Descripción de la promoción")
    private String descripcion;

    @Schema(description = "Tipo de promoción", example = "descuento")
    private String tipo;

    @Schema(description = "Descuento ofrecido, si aplica", example = "50%")
    private String descuento;

    @Schema(description = "Código promocional para canjear, si aplica", example = "NUEVO50")
    private String codigo;

    @Schema(description = "URLs de imágenes de la promoción")
    private List<String> imagenes;

    @Schema(description = "Imagen principal de la promoción")
    private String imagen;

    @Schema(description = "Fecha de inicio", example = "2026-08-15T00:00:00")
    private LocalDateTime fechaInicio;

    @Schema(description = "Fecha de fin", example = "2026-09-15T00:00:00")
    private LocalDateTime fechaFin;

    @Schema(description = "Usos registrados hasta ahora", example = "12")
    private Integer usos;

    @Schema(description = "Máximo de usos (null = ilimitado)", example = "50")
    private Integer usosMaximos;

    @Schema(description = "true si el socio no la ha desactivado manualmente")
    private Boolean activo;

    @Schema(description = "Estado derivado: ACTIVA, PROXIMA, EXPIRADA o DESACTIVADA")
    private String estado;

    @Schema(description = "Fecha de creación de la promoción")
    private LocalDateTime fechaCreacion;
}
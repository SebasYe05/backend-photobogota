package com.photobogota.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO para crear una promoción de un local")
public class CrearPromocionRequestDTO {

    @NotBlank(message = "El local es obligatorio")
    @Schema(description = "ID del local (spot creado por un SOCIO) al que pertenece la promoción", example = "60f5c1d2...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String spotId;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 100, message = "El título no puede exceder los 100 caracteres")
    @Schema(description = "Título de la promoción", example = "50% de descuento en sesión de fotos", requiredMode = Schema.RequiredMode.REQUIRED)
    private String titulo;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 1000, message = "La descripción no puede exceder los 1000 caracteres")
    @Schema(description = "Descripción de la promoción", example = "Promoción especial para nuevos clientes", requiredMode = Schema.RequiredMode.REQUIRED)
    private String descripcion;

    @Schema(description = "Tipo de promoción", example = "descuento")
    private String tipo;

    @Schema(description = "Descuento ofrecido, si aplica", example = "50%")
    private String descuento;

    @Schema(description = "Código promocional para canjear, si aplica", example = "NUEVO50")
    private String codigo;

    @Schema(description = "URLs de imágenes ya subidas via /imagenes/spot")
    private List<String> imagenes;

    @NotBlank(message = "La fecha de inicio es obligatoria")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "La fecha de inicio debe tener formato yyyy-MM-dd")
    @Schema(description = "Fecha de inicio (yyyy-MM-dd)", example = "2026-08-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fechaInicio;

    @NotBlank(message = "La fecha de fin es obligatoria")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "La fecha de fin debe tener formato yyyy-MM-dd")
    @Schema(description = "Fecha de fin (yyyy-MM-dd)", example = "2026-09-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fechaFin;

    @Schema(description = "Máximo de usos/redenciones. null = ilimitado", example = "50")
    private Integer usosMaximos;
}
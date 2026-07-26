package com.photobogota.api.dto;

import java.util.List;

import com.photobogota.api.model.CategoriaReporte;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO para crear un reporte")
public class CrearReporteRequestDTO {

    @NotNull(message = "La categoria es obligatoria")
    @Schema(description = "Categoria del reporte", example = "SPAM")
    private CategoriaReporte categoria;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 1000, message = "La descripcion no puede superar los 1000 caracteres")
    @Schema(description = "Descripcion detallada del reporte", example = "Este spot tiene informacion desactualizada sobre el horario")
    private String descripcion;

    @Schema(description = "ID del spot relacionado, si el reporte aplica a un spot puntual", example = "6a45a990795ab5cec36ccdc8")
    private String spotId;

    @Schema(description = "ID de la reseña puntual dentro del spot, si el reporte aplica a una reseña específica y no al spot en general", example = "r_9f0a1")
    private String resenaId;

    @Schema(description = "URLs de evidencia (capturas de pantalla) ya subidas via /api/v1/imagenes/reporte", example = "[\"/uploads/reportes/abc123.png\"]")
    private List<String> evidencias;
}

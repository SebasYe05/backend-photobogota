package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO para crear una calificacion de spot")
public class CalificacionRequestDTO {

    @Min(1)
    @Max(5)
    @Schema(description = "Calificacion en estrellas", example = "5")
    private Integer estrellas;

    @Size(max = 500, message = "El comentario no puede exceder los 500 caracteres")
    @Schema(description = "Comentario opcional de la calificacion", example = "Excelente spot para fotos")
    private String comentario;
}

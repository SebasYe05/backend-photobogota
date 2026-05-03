package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "DTO para la solicitud de una nueva reseña")
public class ResenaRequestDTO {

    @NotNull
    @Min(1) @Max(5)
    @Schema(description = "Calificación de la reseña", example = "5")
    private Integer rating;

    @NotBlank(message = "El comentario es obligatorio")
    @Size(max = 500, message = "El comentario no puede exceder los 500 caracteres")
    @Schema(description = "Comentario de la reseña", example = "¡Excelente experiencia!")
    private String comentario;
}
package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para agregar un comentario interno de coordinación entre moderadores")
public class ComentarioInternoRequestDTO {

    @NotBlank(message = "El comentario no puede estar vacío")
    @Schema(description = "Texto del comentario interno", example = "Ya llamé al aspirante para confirmar el NIT")
    private String texto;
}

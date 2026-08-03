package com.photobogota.api.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Comentario interno de coordinación entre moderadores")
public class ComentarioInternoDTO {

    @Schema(description = "Nombre de usuario o email del moderador que escribió el comentario")
    private String autor;

    @Schema(description = "Texto del comentario")
    private String texto;

    @Schema(description = "Fecha y hora en la que se escribió el comentario")
    private LocalDateTime fecha;
}

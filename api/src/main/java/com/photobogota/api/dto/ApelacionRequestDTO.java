package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO para que un usuario baneado envíe una apelación")
public class ApelacionRequestDTO {

    @NotBlank(message = "El motivo de la apelación es obligatorio")
    @Schema(description = "Motivo de la apelación", example = "Fue un error, ese contenido era de otra persona")
    private String motivo;
}

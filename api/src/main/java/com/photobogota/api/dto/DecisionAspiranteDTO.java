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
@Schema(description = "DTO para rechazar o solicitar correcciones sobre una solicitud de aspirante, con justificación obligatoria")
public class DecisionAspiranteDTO {

    @NotBlank(message = "Debes indicar el motivo/justificación de la decisión")
    @Schema(description = "Justificación de la decisión, visible para el aspirante", example = "El documento adjunto no es legible, por favor súbelo de nuevo")
    private String motivo;
}

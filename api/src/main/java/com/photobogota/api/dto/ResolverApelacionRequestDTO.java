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
@Schema(description = "DTO para que un admin resuelva una apelación de ban")
public class ResolverApelacionRequestDTO {

    @Schema(description = "true para aprobar la apelación y reactivar la cuenta, false para mantener el ban")
    private Boolean aprobar;

    @NotBlank(message = "La respuesta al usuario es obligatoria")
    @Schema(description = "Respuesta/justificación de la decisión", example = "Apelación aprobada, cuenta reactivada")
    private String respuesta;
}

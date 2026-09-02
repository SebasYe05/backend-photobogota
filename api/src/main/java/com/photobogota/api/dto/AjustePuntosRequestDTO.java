package com.photobogota.api.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para ajuste manual de puntos por parte de un administrador")
public class AjustePuntosRequestDTO {

    @Schema(description = "Delta de puntos a aplicar (positivo o negativo)", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Integer delta;

    @Schema(description = "Motivo del ajuste (opcional, para auditoría)", example = "Bonus por contribución destacada")
    private String motivo;
}

package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO para canjear una promoción (rol MIEMBRO)")
public class CanjearRequestDTO {

    @NotBlank(message = "La promoción es obligatoria")
    @Schema(description = "ID de la promoción a canjear", example = "60f5c1d2...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String promocionId;
}
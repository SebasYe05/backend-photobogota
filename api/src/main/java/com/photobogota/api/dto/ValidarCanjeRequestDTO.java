package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO para validar el código de un canje (rol SOCIO dueño)")
public class ValidarCanjeRequestDTO {

    @NotBlank(message = "El código es obligatorio")
    @Schema(description = "Código de canje presentado por el miembro", example = "XK4P2M9Q", requiredMode = Schema.RequiredMode.REQUIRED)
    private String codigo;

    @NotBlank(message = "El local es obligatorio")
    @Schema(description = "ID del local (spot) del socio que valida", example = "60f5c1d2...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String spotId;
}
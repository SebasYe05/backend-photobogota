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
@Schema(description = "DTO para confirmar la eliminación de cuenta con el código recibido por correo")
public class ConfirmarEliminacionDTO {

    @NotBlank(message = "El código es requerido")
    @Schema(description = "Código de verificación de 6 dígitos", example = "482913")
    private String codigo;
}

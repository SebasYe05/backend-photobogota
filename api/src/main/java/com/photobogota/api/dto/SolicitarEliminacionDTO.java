package com.photobogota.api.dto;

import com.photobogota.api.model.MotivoEliminacionCuenta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO para solicitar la eliminación de la propia cuenta. Ambos campos son opcionales.")
public class SolicitarEliminacionDTO {

    @Schema(description = "Motivo predefinido de la eliminación (opcional)", example = "PREOCUPACIONES_DE_PRIVACIDAD")
    private MotivoEliminacionCuenta motivo;

    @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
    @Schema(description = "Comentario adicional opcional", example = "Ya no visito Bogotá con frecuencia")
    private String comentario;
}

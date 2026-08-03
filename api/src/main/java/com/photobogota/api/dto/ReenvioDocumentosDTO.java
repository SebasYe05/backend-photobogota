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
@Schema(description = "DTO para que el aspirante reenvíe sus documentos tras una solicitud de corrección")
public class ReenvioDocumentosDTO {

    @NotBlank(message = "La ruta del nuevo archivo es obligatoria")
    @Schema(description = "Ruta del nuevo archivo subido", example = "/uploads/aspirantes/abc123.pdf")
    private String rutaArchivo;

    @Schema(description = "Tipo del nuevo archivo subido", example = "pdf")
    private String tipoArchivo;
}

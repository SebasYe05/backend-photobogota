package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "DTO de respuesta para un spot guardado")
public class GuardadoResponseDTO {

    @Schema(description = "ID del guardado")
    private String id;

    @Schema(description = "ID del spot guardado")
    private String spotId;

    @Schema(description = "Fecha en que se guardó")
    private LocalDateTime guardadoEn;
}
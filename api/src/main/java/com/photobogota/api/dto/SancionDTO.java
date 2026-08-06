package com.photobogota.api.dto;

import java.time.LocalDateTime;

import com.photobogota.api.model.TipoSancion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO con la sanción actualmente activa de un usuario")
public class SancionDTO {

    @Schema(description = "Tipo de sanción activa (null si el usuario no tiene sanción activa)")
    private TipoSancion tipo;

    @Schema(description = "Fecha de inicio de la sanción")
    private LocalDateTime fechaInicio;

    @Schema(description = "Fecha de expiración (null para sanciones indefinidas como BAN)")
    private LocalDateTime fechaExpiracion;

    @Schema(description = "Motivo de la sanción")
    private String motivo;

    @Schema(description = "Conteo acumulado de infracciones del usuario")
    private Integer contadorInfracciones;

    @Schema(description = "Indica si la sanción bloquea la publicación de contenido")
    private Boolean bloqueaPublicacion;

    @Schema(description = "Indica si el usuario puede apelar (solo BAN)")
    private Boolean puedeApelar;
}

package com.photobogota.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.photobogota.api.model.TipoPalabra;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO de una palabra o frase prohibida configurada en el filtro de contenido")
public class PalabraProhibidaDTO {

    @Schema(description = "ID de la palabra")
    private String id;

    @Schema(description = "Texto prohibido (palabra o frase)", example = "palabra_prohibida")
    private String texto;

    @Schema(description = "PALABRA (coincidencia de palabra completa) o FRASE (coincidencia de subcadena)")
    private TipoPalabra tipo;

    @Schema(description = "Indica si la regla está activa", example = "true")
    private Boolean activo;

    @Schema(description = "Categoría opcional para agrupar (ej: OFENSIVO, SPAM)", example = "OFENSIVO")
    private String categoria;

    @Schema(description = "Frases de contexto permitidas que neutralizan la palabra (evitan falsos positivos)")
    private List<String> excepciones;

    @Schema(description = "nombreUsuario del admin que creó la regla")
    private String creadoPor;

    @Schema(description = "Fecha de creación de la regla")
    private LocalDateTime fechaCreacion;
}

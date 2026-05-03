package com.photobogota.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "DTO para crear un nuevo spot")
public class CrearSpotRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Schema(description = "Nombre del spot", example = "Parque Central")
    private String nombre;

    @NotNull(message = "La latitud es obligatoria")
    @Schema(description = "Latitud del spot", example = "4.6097")
    private Double latitud;

    @NotNull(message = "La longitud es obligatoria")
    @Schema(description = "Longitud del spot", example = "-74.0817")
    private Double longitud;

    @NotBlank(message = "La dirección es obligatoria")
    @Schema(description = "Dirección del spot", example = "Calle 123 #45-67")
    private String direccion;

    @NotBlank(message = "La categoría es obligatoria")
    @Schema(description = "Categoría del spot", example = "Parque, Monumento, Mirador, etc.")
    private String categoria;

    @NotBlank(message = "La localidad es obligatoria")
    @Schema(description = "Localidad del spot", example = "Kennedy, Chapinero, etc.")
    private String localidad;

    @NotBlank(message = "La descripción es obligatoria")
    @Schema(description = "Descripción del spot", example = "Lindo parque en el centro de la ciudad")
    private String descripcion;

    @Schema(description = "Recomendación para visitar el spot", example = "Ideal para paseos matutinos")
    private String recomendacion;

    @Schema(description = "Consejos para tomar fotos en el spot", example = "Usa el ángulo de la luz del sol para obtener mejores imágenes")
    private String tipsFoto;

    @Schema(description = "Lista de URLs de imágenes del spot", example = "[\"/spots/imagen1.jpg\", \"/spots/imagen2.jpg\"]")
    private List<String> imagenes; // URLs ya subidas, o vacío
}
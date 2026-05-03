package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO para la entidad Localidades de Bogotá")
public class LocalidadDTO {

    @Schema(description = "Identificador único de la localidad")
    private String id;

    @Schema(description = "Nombre de la localidad", example = "Kennedy")
    private String nombre;

    @Schema(description = "Descripción de la localidad", example = "Localidad en el barrio Kennedy")
    private String descripcion;

    @Schema(description = "Ruta de la imagen de la localidad", example = "/localidades/kennedy.jpg")
    private String imagen;

    @Schema(description = "Indica si la localidad está activa", example = "true")
    private Boolean activo;
}
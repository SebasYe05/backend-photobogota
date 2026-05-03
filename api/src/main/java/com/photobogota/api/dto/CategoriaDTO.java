package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO para la categoría de fotografías")
public class CategoriaDTO {

    @Schema(description = "ID de la categoría")
    private String id;

    @Schema(description = "Nombre de la categoría", example = "Paisaje urbano")
    private String nombre;

    @Schema(description = "Descripción de la categoría", example = "Fotografías que capturan la esencia de la ciudad y su arquitectura")
    private String descripcion;

    @Schema(description = "Imagen de la categoría", example = "/categorias/imagen-categoria.jpg")
    private String imagen;

    @Schema(description = "Indicador de si la categoría está activa", example = "true")
    private Boolean activo;
}
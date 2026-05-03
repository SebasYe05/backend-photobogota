package com.photobogota.api.dto;

import lombok.Data;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "DTO que representa un resumen de un spot fotográfico")
public class SpotResumenDTO {

    @Schema(description = "Identificador único del spot")
    private String id;
    
    @Schema(description = "Nombre del spot fotográfico", example = "Parque Simón Bolívar")
    private String nombre;

    @Schema(description = "Latitud del spot", example = "3.8667")
    private Double latitud;

    @Schema(description = "Longitud del spot", example = "-73.0500")
    private Double longitud;

    @Schema(description = "Dirección del spot", example = "Calle 63 #68-95, Bogotá")
    private String direccion;

    @Schema(description = "Categoría del spot", example = "Parque")
    private String categoria;

    @Schema(description = "Localidad del spot", example = "Kennedy")
    private String localidad;

    @Schema(description = "Descripción del spot", example = "Parque ubicado en el corazón de Bogotá")
    private String descripcion;

    @Schema(description = "Recomendación para fotografiar el spot", example = "Ideal para fotos de paisaje al atardecer")
    private String recomendacion;

    @Schema(description = "Consejos para tomar fotos en el spot", example = "Evitar la luz directa del sol")
    private String tipsFoto;

    @Schema(description = "Lista de imágenes del spot", example = "[\"/images/spot1.jpg\", \"/images/spot2.jpg\"]")
    private List<String> imagenes;
    
    @Schema(description = "Imagen principal del spot", example = "/images/spot1.jpg")
    private String imagen;

    @Schema(description = "Calificación del spot", example = "4.5")
    private Double rating;

    @Schema(description = "Total de reseñas del spot", example = "100")
    private Integer totalResenas;

    @Schema(description = "ID del usuario que creó el spot", example = "user123")
    private String usuarioId;

    @Schema(description = "Fecha de creación del spot", example = "2023-01-01T00:00:00Z")
    private String createdAt;
}
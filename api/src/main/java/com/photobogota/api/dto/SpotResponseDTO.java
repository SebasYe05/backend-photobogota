package com.photobogota.api.dto;

import lombok.Data;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "DTO para la respuesta de un spot fotográfico")
public class SpotResponseDTO {

    @Schema(description = "ID del spot")
    private String id;

    @Schema(description = "Nombre del spot", example = "Parque Simón Bolívar")
    private String nombre;

    @Schema(description = "Latitud del spot", example = "3.8667")
    private Double latitud;

    @Schema(description = "Longitud del spot", example = "-74.0000")
    private Double longitud;

    @Schema(description = "Dirección del spot", example = "Calle 123 #45-67")
    private String direccion;

    @Schema(description = "Categoría del spot", example = "Parque, Monumento, etc.")
    private String categoria;

    @Schema(description = "Localidad del spot", example = "Kennedy, Chapinero, etc.")
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

    @Schema(description = "Calificación del spot", example = "4")
    private Double rating;

    @Schema(description = "Número total de reseñas del spot", example = "10")
    private Integer totalResenas;

    @Schema(description = "ID del usuario que creó el spot")
    private String usuarioId;

     @Schema(description = "Fecha de creación del spot", example = "2023-01-01T00:00:00Z")
    private String createdAt;

    @Schema(description = "Lista de reseñas del spot", example = "[{ \"id\": \"1\", \"usuario\": \"Maria Gomez\", \"avatar\": \"/avatars/maria.jpg\", \"rating\": 5, \"comentario\": \"¡Excelente spot para fotos!\", \"fecha\": \"Hace 2 días\" }]")
    private List<ResenaResponseDTO> resenas;

    @Data
    @Schema(description = "DTO para la respuesta de una reseña de un spot")
    public static class ResenaResponseDTO {

        @Schema(description = "ID de la reseña")
        private String id;

        @Schema(description = "Nombre del usuario que hizo la reseña", example = "Maria Gomez")
        private String usuario;

        @Schema(description = "Avatar del usuario que hizo la reseña", example = "/avatars/maria.jpg")
        private String avatar;

        @Schema(description = "Calificación dada por el usuario", example = "5")
        private Integer rating;

        @Schema(description = "Comentario de la reseña", example = "¡Excelente spot para fotos!")
        private String comentario;

        @Schema(description = "Fecha de la reseña en formato legible", example = "Hace 2 días")
        private String fecha; // formateado, ej: "Hace 2 días"
    }
}
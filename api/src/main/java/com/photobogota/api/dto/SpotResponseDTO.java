package com.photobogota.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class SpotResponseDTO {
    private String id;
    private String nombre;
    private Double latitud;
    private Double longitud;
    private String direccion;
    private String categoria;
    private String localidad;
    private String descripcion;
    private String recomendacion;
    private String tipsFoto;
    private List<String> imagenes;
    private String imagen;          // primera imagen (comodidad para el front)
    private Double rating;
    private Integer totalResenas;
    private String creadorUsername;
    private List<ResenaResponseDTO> resenas;

    @Data
    public static class ResenaResponseDTO {
        private String id;
        private String usuario;
        private String avatar;
        private Integer rating;
        private String comentario;
        private String fecha;       // formateado, ej: "Hace 2 días"
    }
}
package com.photobogota.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class SpotResumenDTO {
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
    private String imagen;
    private Double rating;
    private Integer totalResenas;
    private String usuarioId;
    private String createdAt;
}
package com.photobogota.api.dto;

import lombok.Data;

@Data
public class SpotResumenDTO {
    private String id;
    private String nombre;
    private Double latitud;
    private Double longitud;
    private String direccion;
    private String categoria;
    private String localidad;
    private String imagen;   // primera imagen
    private Double rating;
    private Integer totalResenas;
}
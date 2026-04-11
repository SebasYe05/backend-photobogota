package com.photobogota.api.dto;

import lombok.Data;

@Data
public class LocalidadDTO {
    private String id;
    private String nombre;
    private String descripcion;
    private String imagen;
    private Boolean activo;
}
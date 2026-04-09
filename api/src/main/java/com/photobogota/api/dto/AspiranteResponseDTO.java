package com.photobogota.api.dto;

import java.time.LocalDate;

import com.photobogota.api.model.EstadoAspirante;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AspiranteResponseDTO {

    private String id;
    private String nombres;
    private String apellidos;
    private String email;
    private String telefono;
    private String direccion;
    private String nit;
    private LocalDate fechaNacimiento;
    private String nombrePropietario;
    private String razonSocial;
    private String categoria;
    private String localidad;
    private String rutaArchivo;
    private String tipoArchivo;
    private EstadoAspirante estado;
    private LocalDate fechaSolicitud;
    private String codigo;
}
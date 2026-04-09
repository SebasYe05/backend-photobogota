package com.photobogota.api.model;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "aspirantes_socios")
public class Aspirante {

    @Id
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

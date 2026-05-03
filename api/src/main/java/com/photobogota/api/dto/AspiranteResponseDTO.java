package com.photobogota.api.dto;

import java.time.LocalDate;

import com.photobogota.api.model.EstadoAspirante;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO de respuesta para un aspirante")
public class AspiranteResponseDTO {

    @Schema(description = "ID del aspirante")
    private String id;

    @Schema(description = "Nombres del aspirante", example = "Juan Pablo")
    private String nombres;

    @Schema(description = "Apellidos del aspirante", example = "Marin")
    private String apellidos;

    @Schema(description = "Correo electrónico del aspirante", example = "juanpablo@example.com")
    private String email;

    @Schema(description = "Número de teléfono del aspirante", example = "3001234567")
    private String telefono;

    @Schema(description = "Dirección del aspirante", example = "Calle 123 #45-67")
    private String direccion;

    @Schema(description = "NIT del aspirante", example = "800123456-7")
    private String nit;

    @Schema(description = "Fecha de nacimiento", example = "1988-09-10")
    private LocalDate fechaNacimiento;

    @Schema(description = "Nombre del propietario del negocio", example = "Juan Pablo Marin")
    private String nombrePropietario;

    @Schema(description = "Razón social del negocio", example = "Cafetería Marin S.A.S.")
    private String razonSocial;

    @Schema(description = "Categoría del aspirante", example = "Cafetería")
    private String categoria;

    @Schema(description = "Localidad del aspirante", example = "Kennedy")
    private String localidad;

    @Schema(description = "Ruta del archivo adjunto", example = "/uploads/documento.pdf")
    private String rutaArchivo;

    @Schema(description = "Tipo de archivo adjunto", example = "pdf")
    private String tipoArchivo;

    @Schema(description = "Estado del aspirante", example = "Pendiente")
    private EstadoAspirante estado;

    @Schema(description = "Fecha de solicitud", example = "2026-05-03")
    private LocalDate fechaSolicitud;

    @Schema(description = "Código del aspirante", example = "ASPIRANTE001")
    private String codigo;
}
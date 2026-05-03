package com.photobogota.api.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para la solicitud de aspirante a socio")
public class SolicitudAspiranteDTO {

    @NotBlank(message = "Los nombres son obligatorios")
    @Schema(description = "Nombres del aspirante", example = "Juan Sebastian")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Schema(description = "Apellidos del aspirante", example = "Romero Ramirez")
    private String apellidos;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Schema(description = "Email del aspirante", example = "juan.romero@example.com")
    private String email;

    @Schema(description = "Número de teléfono del aspirante", example = "3001234567")
    private String telefono;

    @Schema(description = "Dirección del aspirante", example = "Calle 123 #45-67")
    private String direccion;

    @NotBlank(message = "El NIT/RUT es obligatorio")
    @Schema(description = "NIT o RUT del aspirante", example = "123456789")
    private String nit;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Schema(description = "Fecha de nacimiento del aspirante", example = "1990-01-01")
    private LocalDate fechaNacimiento;

    @Schema(description = "Nombre del propietario del negocio, si aplica", example = "Juan Sebastian Romero Ramirez")
    private String nombrePropietario;

    @Schema(description = "Razón social de la empresa", example = "Empresa de Prueba S.A.")
    private String razonSocial;

    @Schema(description = "Categoría del establecimiento", example = "Bar, Restaurante, Café, etc.")
    private String categoria;

    @Schema(description = "Localidad del aspirante", example = "Kennedy, Chapinero, etc.")
    private String localidad;

    @Schema(description = "Ruta del archivo de la cédula del aspirante", example = "/uploads/cedula_juan_romero.pdf")
    private String rutaArchivo;

    @Schema(description = "Tipo de archivo de la cédula del aspirante", example = "pdf")
    private String tipoArchivo;
}
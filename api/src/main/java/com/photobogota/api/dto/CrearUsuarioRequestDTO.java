package com.photobogota.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrearUsuarioRequestDTO {

    @NotBlank
    private String nombresCompletos;
    @Email
    private String email;

    @NotBlank
    private String nombreUsuario;

    @NotBlank
    @Size(min = 8, message = "La seguridad es primero: mínimo 8 caracteres")
    private String contrasena;

    @NotNull(message = "La fecha de nacimiento es requerida")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    @MayorDe18(message = "Debes ser mayor de 18 años para registrarte")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El rol es requerido")
    private String rol; // ADMIN, MOD, SOCIO, MIEMBRO
}
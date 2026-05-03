package com.photobogota.api.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "DTO para el registro de un nuevo usuario")
public class RegistroRequestDTO {
    @NotBlank
    @Schema(description = "Nombres completos", example = "Juan Romero")
    private String nombresCompletos;

    @Email
    @Schema(description = "Email", example = "juan.romero@example.com")
    private String email;

    @NotBlank
    @Schema(description = "Nombre de usuario único", example = "juanromero")
    private String nombreUsuario;

    @NotBlank
    @Schema(description = "Contraseña", example = "Segura123.")
    @Size(min = 8, message = "La seguridad es primero: mínimo 8 caracteres")
    private String contrasena;

    @NotNull(message = "La fecha de nacimiento es requerida")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    @MayorDe18(message = "Debes ser mayor de 18 años para registrarte")
    @Schema(description = "Fecha de nacimiento", example = "1990-05-15")
    private LocalDate fechaNacimiento;
}

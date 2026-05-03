package com.photobogota.api.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO para crear un nuevo usuario")
public class CrearUsuarioRequestDTO {

    @NotBlank
    @Schema(description = "Nombres completos del usuario", example = "Daniel Felipe Cruz Reyes")
    private String nombresCompletos;
    
    @Email
    @Schema(description = "Correo electrónico del usuario", example = "daniel.felipe@example.com")
    private String email;

    @NotBlank
    @Schema(description = "Nombre de usuario", example = "daniel_felipe")
    private String nombreUsuario;

    @NotBlank
    @Size(min = 8, message = "La seguridad es primero: mínimo 8 caracteres")
    @Schema(description = "Contraseña del usuario", example = "Seguro123.")
    private String contrasena;

    @NotNull(message = "La fecha de nacimiento es requerida")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    @MayorDe18(message = "Debes ser mayor de 18 años para registrarte")
    @Schema(description = "Fecha de nacimiento del usuario", example = "2004-11-05")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El rol es requerido")
    @Schema(description = "Rol del usuario", example = "SOCIO")
    private String rol;
}
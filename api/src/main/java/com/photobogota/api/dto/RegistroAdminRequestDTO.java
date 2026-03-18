package com.photobogota.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroAdminRequestDTO {
    
    @NotBlank(message = "Los nombres completos son requeridos")
    private String nombresCompletos;
    
    @Email(message = "El email debe ser válido")
    @NotBlank(message = "El email es requerido")
    private String email;

    @NotBlank(message = "El nombre de usuario es requerido")
    private String nombreUsuario;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres")
    private String contrasena;
    
    @NotBlank(message = "El teléfono es requerido")
    private String telefono;
    
    private String biografia;
}

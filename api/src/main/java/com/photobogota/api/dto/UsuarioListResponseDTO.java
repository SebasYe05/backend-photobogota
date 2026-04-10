package com.photobogota.api.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioListResponseDTO {
    private String id;
    private String nombresCompletos;
    private String email;
    private String nombreUsuario;
    private String rol;
    private Boolean estadoCuenta;
    private LocalDateTime fechaRegistro;
}

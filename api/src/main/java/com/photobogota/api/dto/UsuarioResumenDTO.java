package com.photobogota.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsuarioResumenDTO {
    private String nombreUsuario;
    private String nombresCompletos;
    private String fotoPerfil;
    private String rol;
    private Integer nivel;
}

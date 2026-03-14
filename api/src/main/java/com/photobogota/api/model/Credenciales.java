package com.photobogota.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Credenciales {
    private String nombreUsuario;
    private String email;
    private String contrasena;
    private Rol rol;
}

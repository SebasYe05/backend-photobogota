package com.photobogota.api.service;

import com.photobogota.api.dto.CambiarContrasenaDTO;
import com.photobogota.api.dto.CambiarContrasenaResponseDTO;
import com.photobogota.api.dto.EditarPerfilDTO;
import com.photobogota.api.dto.PerfilUsuarioDTO;

public interface IUsuarioService {

    PerfilUsuarioDTO editarPerfil(String nombreUsuario, EditarPerfilDTO dto);
    
    PerfilUsuarioDTO obtenerPerfil(String nombreUsuario);
    
    CambiarContrasenaResponseDTO cambiarContrasena(String nombreUsuario, CambiarContrasenaDTO dto);
}

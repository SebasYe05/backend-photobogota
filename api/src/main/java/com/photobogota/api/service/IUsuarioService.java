package com.photobogota.api.service;

import com.photobogota.api.dto.EditarPerfilDTO;
import com.photobogota.api.dto.PerfilUsuarioDTO;

public interface IUsuarioService {

    PerfilUsuarioDTO editarPerfil(String nombreUsuario, EditarPerfilDTO dto);
    
    PerfilUsuarioDTO obtenerPerfil(String nombreUsuario);
}

package com.photobogota.api.service;

import com.photobogota.api.dto.PerfilUsuarioDTO;
import com.photobogota.api.dto.RegistroRequestDTO;

public interface IUsuarioService {
    PerfilUsuarioDTO registrar(RegistroRequestDTO dto);
}

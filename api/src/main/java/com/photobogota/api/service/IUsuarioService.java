package com.photobogota.api.service;

import com.photobogota.api.dto.RegistroRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;

public interface IUsuarioService {
    RegistroResponseDTO registrar(RegistroRequestDTO dto);
}

package com.photobogota.api.service;

import com.photobogota.api.dto.RegistroAdminRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;

public interface IAdminService {
    
    RegistroResponseDTO registrarAdmin(RegistroAdminRequestDTO dto);
}

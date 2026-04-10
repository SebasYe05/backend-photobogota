package com.photobogota.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.photobogota.api.dto.CrearUsuarioRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;
import com.photobogota.api.dto.UsuarioListResponseDTO;

public interface IAdminService {

    RegistroResponseDTO crearUsuario(CrearUsuarioRequestDTO dto);

    Page<UsuarioListResponseDTO> listarUsuarios(Pageable pageable);

    void actualizarEstadoUsuario(String usuarioId, boolean activo);

    void eliminarUsuario(String usuarioId);
}

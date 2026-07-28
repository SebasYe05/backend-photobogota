package com.photobogota.api.service;

import com.photobogota.api.dto.CambiarContrasenaDTO;
import com.photobogota.api.dto.CambiarContrasenaResponseDTO;
import com.photobogota.api.dto.EditarPerfilDTO;
import com.photobogota.api.dto.PerfilUsuarioDTO;
import com.photobogota.api.dto.ResenaDTO;
import com.photobogota.api.dto.SpotResponseDTO;

import java.util.List;

public interface IUsuarioService {

    PerfilUsuarioDTO editarPerfil(String nombreUsuario, EditarPerfilDTO dto);

    PerfilUsuarioDTO obtenerPerfil(String nombreUsuario);

    CambiarContrasenaResponseDTO cambiarContrasena(String nombreUsuario, CambiarContrasenaDTO dto);

    List<SpotResponseDTO> obtenerSpotsDeUsuario(String nombreUsuario);

    List<ResenaDTO> obtenerResenasDeUsuario(String nombreUsuario);

    List<SpotResponseDTO> obtenerGuardados(String nombreUsuario);
}

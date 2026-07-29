package com.photobogota.api.service;

import com.photobogota.api.dto.CambiarContrasenaDTO;
import com.photobogota.api.dto.CambiarContrasenaResponseDTO;
import com.photobogota.api.dto.EditarPerfilDTO;
import com.photobogota.api.dto.PerfilUsuarioDTO;
import com.photobogota.api.dto.SpotResumenDTO;
import com.photobogota.api.dto.CalificacionResponseDTO;

import java.util.List;

public interface IUsuarioService {

    PerfilUsuarioDTO editarPerfil(String nombreUsuario, EditarPerfilDTO dto);

    PerfilUsuarioDTO obtenerPerfil(String nombreUsuario);

    CambiarContrasenaResponseDTO cambiarContrasena(String nombreUsuario, CambiarContrasenaDTO dto);

    List<SpotResumenDTO> obtenerSpotsDeUsuario(String nombreUsuario);

    List<CalificacionResponseDTO> obtenerResenasDeUsuario(String nombreUsuario);

    /** Spots guardados por el usuario (mismo shape que GET /spots) */
    List<SpotResumenDTO> obtenerGuardados(String nombreUsuario);

    /** Idempotente: si el spot ya estaba guardado, no duplica y devuelve el existente */
    SpotResumenDTO guardarSpot(String nombreUsuario, String spotId);

    /** Idempotente: si no existía el guardado, no falla */
    void quitarGuardado(String nombreUsuario, String spotId);
}

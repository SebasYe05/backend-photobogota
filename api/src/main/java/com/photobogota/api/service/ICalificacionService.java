package com.photobogota.api.service;

import com.photobogota.api.dto.CalificacionRequestDTO;
import com.photobogota.api.dto.CalificacionResponseDTO;

import java.util.List;

public interface ICalificacionService {

    CalificacionResponseDTO crearCalificacion(String spotId, CalificacionRequestDTO request, String usuario);

    CalificacionResponseDTO modificarCalificacion(String spotId, String calificacionId, CalificacionRequestDTO request, String usuario);

    List<CalificacionResponseDTO> listarPorSpot(String spotId);

    CalificacionResponseDTO obtenerPorId(String id);
}

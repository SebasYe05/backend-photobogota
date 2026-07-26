package com.photobogota.api.service;

import java.util.List;

import com.photobogota.api.dto.CrearReporteRequestDTO;
import com.photobogota.api.dto.ReporteResponseDTO;
import com.photobogota.api.model.Rol;

public interface IReporteService {

    ReporteResponseDTO crearReporte(CrearReporteRequestDTO request, String usuario);

    ReporteResponseDTO obtenerPorId(String id);

    List<ReporteResponseDTO> listarMisReportes(String usuario);

    List<ReporteResponseDTO> listarPorRolAsignado(Rol rol);
}

package com.photobogota.api.service;

import java.util.List;

import com.photobogota.api.dto.AspiranteResponseDTO;
import com.photobogota.api.dto.SolicitudAspiranteDTO;
import com.photobogota.api.model.EstadoAspirante;

public interface IAspiranteService {

    AspiranteResponseDTO crearSolicitud(SolicitudAspiranteDTO request);

    AspiranteResponseDTO obtenerPorId(String id);

    AspiranteResponseDTO obtenerPorEmail(String email);

    AspiranteResponseDTO obtenerPorCodigo(String codigo);

    List<AspiranteResponseDTO> obtenerTodos();

    List<AspiranteResponseDTO> obtenerPorEstado(EstadoAspirante estado);

    AspiranteResponseDTO aprobarAspirante(String id);

    AspiranteResponseDTO rechazarAspirante(String id);

    AspiranteResponseDTO actualizarEstado(String id, EstadoAspirante estado);
}

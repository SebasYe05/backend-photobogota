package com.photobogota.api.service;

import java.util.Map;

import com.photobogota.api.dto.PuntosResponseDTO;

public interface IPuntosService {

    int sumarPuntos(String nombreUsuario, com.photobogota.api.model.TipoPuntos tipo, String refId);

    PuntosResponseDTO obtenerPuntos(String nombreUsuario);

    Map<String, String> obtenerConfig();

    Map<String, String> actualizarConfig(Map<String, String> config);

    PuntosResponseDTO ajustarPuntosPorId(String usuarioId, int delta, String motivo);
}

package com.photobogota.api.service;

import java.util.List;

import com.photobogota.api.dto.AspiranteResponseDTO;
import com.photobogota.api.dto.EstadisticasAspiranteDTO;
import com.photobogota.api.dto.ReenvioDocumentosDTO;
import com.photobogota.api.dto.SolicitudAspiranteDTO;
import com.photobogota.api.model.EstadoAspirante;

public interface IAspiranteService {

    AspiranteResponseDTO crearSolicitud(SolicitudAspiranteDTO request);

    AspiranteResponseDTO obtenerPorId(String id);

    AspiranteResponseDTO obtenerPorEmail(String email);

    AspiranteResponseDTO obtenerPorCodigo(String codigo);

    List<AspiranteResponseDTO> obtenerTodos();

    List<AspiranteResponseDTO> obtenerPorEstado(EstadoAspirante estado);

    AspiranteResponseDTO aprobarAspirante(String id, String responsable);

    AspiranteResponseDTO enviarCredenciales(String id, String responsable);

    AspiranteResponseDTO rechazarAspirante(String id, String motivo, String responsable);

    AspiranteResponseDTO solicitarCorreccion(String id, String motivo, String responsable);

    AspiranteResponseDTO reenviarDocumentos(String codigo, ReenvioDocumentosDTO request);

    AspiranteResponseDTO agregarComentarioInterno(String id, String texto, String autor);

    EstadisticasAspiranteDTO obtenerEstadisticas();

    AspiranteResponseDTO actualizarEstado(String id, EstadoAspirante estado);
}

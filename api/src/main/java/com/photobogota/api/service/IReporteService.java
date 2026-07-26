package com.photobogota.api.service;

import java.util.List;

import com.photobogota.api.dto.CambiarEstadoRequestDTO;
import com.photobogota.api.dto.CrearReporteRequestDTO;
import com.photobogota.api.dto.EscalarReporteRequestDTO;
import com.photobogota.api.dto.ReporteResponseDTO;
import com.photobogota.api.model.CategoriaReporte;
import com.photobogota.api.model.EstadoReporte;
import com.photobogota.api.model.Gravedad;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.TipoObjetivoReporte;

public interface IReporteService {

    ReporteResponseDTO crearReporte(CrearReporteRequestDTO request, String usuario);

    ReporteResponseDTO obtenerPorId(String id);

    List<ReporteResponseDTO> listarMisReportes(String usuario);

    List<ReporteResponseDTO> listarPorRolAsignado(Rol rol);

    /**
     * Dashboard de reportes de la Etapa 2, con filtros combinables.
     * Si rolUsuario = ADMIN, ve todos los reportes (oversight total).
     * Si rolUsuario = MOD, ve solo los reportes asignados a MOD.
     * Todos los filtros son opcionales (null = sin filtrar por ese campo).
     *
     * @param orden "recientes" (default), "antiguos" o "prioridad"
     *              (gravedad más crítica primero, y dentro de la misma
     *              gravedad los más antiguos sin atender primero).
     */
    List<ReporteResponseDTO> obtenerDashboard(
            Rol rolUsuario,
            EstadoReporte estado,
            Gravedad gravedad,
            CategoriaReporte categoria,
            TipoObjetivoReporte tipoObjetivo,
            Boolean escalado,
            String orden);

    ReporteResponseDTO cambiarEstado(String id, CambiarEstadoRequestDTO request, String usuario, Rol rolUsuario);

    ReporteResponseDTO escalarReporte(String id, EscalarReporteRequestDTO request, String usuario, Rol rolUsuario);
}

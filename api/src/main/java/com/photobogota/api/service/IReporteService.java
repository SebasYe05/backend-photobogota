package com.photobogota.api.service;

import java.util.List;

import com.photobogota.api.dto.CambiarEstadoRequestDTO;
import com.photobogota.api.dto.CrearReporteRequestDTO;
import com.photobogota.api.dto.EscalarReporteRequestDTO;
import com.photobogota.api.dto.ReporteResponseDTO;
import com.photobogota.api.dto.ValidarReporteRequestDTO;
import com.photobogota.api.model.CategoriaReporte;
import com.photobogota.api.model.EstadoReporte;
import com.photobogota.api.model.Gravedad;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.TipoObjetivoReporte;

public interface IReporteService {

    ReporteResponseDTO crearReporte(CrearReporteRequestDTO request, String usuario);

    ReporteResponseDTO obtenerPorId(String id);

    List<ReporteResponseDTO> listarMisReportes(String usuario);

    /**
     * Cola de reportes asignados a un rol. Para SOCIO, además hay que
     * filtrar por el dueño (un socio no debe ver los locales de otro), por
     * eso se pasa "username": para MOD/ADMIN se ignora.
     */
    List<ReporteResponseDTO> listarPorRolAsignado(Rol rol, String username);

    /**
     * Dashboard de reportes de la Etapa 2, con filtros combinables.
     * Si rolUsuario = ADMIN, ve los reportes asignados a ADMIN (incluye lo
     * escalado por moderación). Si rolUsuario = MOD, ve solo lo asignado a
     * MOD. Si rolUsuario = SOCIO, ve solo lo asignado a SOCIO y de sus
     * propios locales (se filtra por "username").
     * Todos los filtros son opcionales (null = sin filtrar por ese campo).
     *
     * @param orden "recientes" (default), "antiguos" o "prioridad"
     *              (gravedad más crítica primero, y dentro de la misma
     *              gravedad los más antiguos sin atender primero).
     */
    List<ReporteResponseDTO> obtenerDashboard(
            Rol rolUsuario,
            String username,
            EstadoReporte estado,
            Gravedad gravedad,
            CategoriaReporte categoria,
            TipoObjetivoReporte tipoObjetivo,
            Boolean escalado,
            String orden);

    ReporteResponseDTO cambiarEstado(String id, CambiarEstadoRequestDTO request, String usuario, Rol rolUsuario);

    /**
     * Escala un reporte al siguiente nivel de la cadena SOCIO -> MOD ->
     * ADMIN (HU 24). Un SOCIO solo puede escalar lo suyo (a MOD), un MOD
     * solo lo suyo (a ADMIN). ADMIN es el tope y no puede escalar más.
     */
    ReporteResponseDTO escalarReporte(String id, EscalarReporteRequestDTO request, String usuario, Rol rolUsuario);

    /**
     * Reportes marcados como "Solucionado" por un SOCIO o un ADMIN que
     * están esperando que un MOD valide la solución antes de notificar al
     * miembro afectado (HU 15 pt 4-5, HU 16 pt 4-5).
     */
    List<ReporteResponseDTO> listarPendientesValidacion();

    /**
     * Un MOD aprueba o rechaza la solución propuesta. Si aprueba, el
     * reporte pasa a RESUELTO y se notifica automáticamente al miembro que
     * lo reportó. Si rechaza, vuelve a EN_REVISION para que quien lo
     * resolvió lo revise de nuevo.
     */
    ReporteResponseDTO validarReporte(String id, ValidarReporteRequestDTO request, String usuarioMod);
}

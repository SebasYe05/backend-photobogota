package com.photobogota.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.photobogota.api.dto.MetricasEliminacionDTO;
import com.photobogota.api.dto.ProcesarEliminacionAdminDTO;
import com.photobogota.api.dto.SolicitudEliminacionAdminDTO;
import com.photobogota.api.model.EstadoSolicitudEliminacion;

/**
 * Servicio para que un ADMIN gestione las solicitudes de eliminación de
 * cuenta: verificar identidad, resolver dependencias, forzar la
 * anonimización inmediata, rechazar solicitudes en disputa y consultar
 * métricas agregadas.
 */
public interface IAdminEliminacionCuentaService {

    /**
     * Lista paginada de solicitudes de eliminación, opcionalmente filtrada
     * por estado. Cada elemento incluye la verificación automática de
     * identidad y un resumen de las dependencias pendientes.
     */
    Page<SolicitudEliminacionAdminDTO> listarSolicitudes(EstadoSolicitudEliminacion estado, Pageable pageable);

    /**
     * Detalle completo de una solicitud puntual.
     */
    SolicitudEliminacionAdminDTO obtenerDetalle(String solicitudId);

    /**
     * Fuerza el procesamiento inmediato de una solicitud PROGRAMADA (o
     * PENDIENTE_VERIFICACION, si el admin decide saltarse la verificación por
     * correo): resuelve dependencias, anonimiza los datos personales,
     * notifica a las partes afectadas y marca la solicitud como COMPLETADA.
     */
    String procesarInmediatamente(String solicitudId, String adminUsername, ProcesarEliminacionAdminDTO dto);

    /**
     * Rechaza/cancela una solicitud (por ejemplo, ante una disputa) y
     * reactiva la cuenta del usuario.
     */
    String rechazarSolicitud(String solicitudId, String adminUsername, ProcesarEliminacionAdminDTO dto);

    /**
     * Métricas agregadas de todas las solicitudes: por estado, por motivo,
     * por rol y tiempos de procesamiento.
     */
    MetricasEliminacionDTO obtenerMetricas();
}

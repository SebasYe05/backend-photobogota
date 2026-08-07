package com.photobogota.api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.photobogota.api.model.EstadoReporte;
import com.photobogota.api.model.Reporte;
import com.photobogota.api.model.Rol;

public interface ReporteRepository extends MongoRepository<Reporte, String> {

    boolean existsByNumeroTicket(String numeroTicket);

    List<Reporte> findByReportadoPor(String reportadoPor);

    List<Reporte> findByAsignadoA(Rol asignadoA);

    List<Reporte> findByAsignadoAAndEstado(Rol asignadoA, EstadoReporte estado);

    // Cola propia de un SOCIO: solo lo asignado a SOCIO y de sus locales.
    List<Reporte> findByAsignadoAAndPropietarioSocio(Rol asignadoA, String propietarioSocio);

    // Reportes pendientes de que un MOD valide una solución propuesta por
    // un SOCIO o un ADMIN (HU 15 pt 4-5, HU 16 pt 4-5).
    List<Reporte> findByEstado(EstadoReporte estado);

    // Reportes asignados a un SOCIO que siguen NUEVO y ya vencieron su plazo
    // de respuesta de 24h, para el escalamiento automático (HU 15 pt 7).
    List<Reporte> findByAsignadoAAndEstadoAndFechaLimiteRespuestaBefore(
            Rol asignadoA, EstadoReporte estado, LocalDateTime ahora);

    // Reincidencia: cuántos reportes activos (no resueltos/rechazados) existen
    // ya sobre el mismo spot o la misma reseña. Se usa para subir la gravedad
    // automáticamente cuando un mismo objetivo se reporta varias veces.
    long countBySpotIdAndEstadoIn(String spotId, List<EstadoReporte> estados);

    long countByResenaIdAndEstadoIn(String resenaId, List<EstadoReporte> estados);

    // ── Etapa 2 (eliminación de cuenta): dependencias pendientes ──

    // Reportes que el propio usuario presentó y siguen pendientes.
    List<Reporte> findByReportadoPorAndEstadoIn(String reportadoPor, List<EstadoReporte> estados);

    // Reportes pendientes sobre spots creados por el usuario.
    List<Reporte> findBySpotIdInAndEstadoIn(List<String> spotIds, List<EstadoReporte> estados);

    // Reportes pendientes sobre reseñas escritas por el usuario.
    List<Reporte> findByAutorResenaReportadaAndEstadoIn(String autorResenaReportada, List<EstadoReporte> estados);
}

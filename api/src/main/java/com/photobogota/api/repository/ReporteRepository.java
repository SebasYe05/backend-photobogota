package com.photobogota.api.repository;

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

    // Reincidencia: cuántos reportes activos (no resueltos/rechazados) existen
    // ya sobre el mismo spot o la misma reseña. Se usa para subir la gravedad
    // automáticamente cuando un mismo objetivo se reporta varias veces.
    long countBySpotIdAndEstadoIn(String spotId, List<EstadoReporte> estados);

    long countByResenaIdAndEstadoIn(String resenaId, List<EstadoReporte> estados);
}

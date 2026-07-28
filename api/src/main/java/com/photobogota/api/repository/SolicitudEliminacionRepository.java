package com.photobogota.api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.photobogota.api.model.EstadoSolicitudEliminacion;
import com.photobogota.api.model.SolicitudEliminacionCuenta;

public interface SolicitudEliminacionRepository extends MongoRepository<SolicitudEliminacionCuenta, ObjectId> {

    /**
     * Busca si el usuario ya tiene una solicitud activa (pendiente de
     * verificación o programada). Se usa para evitar solicitudes duplicadas.
     */
    Optional<SolicitudEliminacionCuenta> findByUsuarioIdAndEstadoIn(ObjectId usuarioId,
            List<EstadoSolicitudEliminacion> estados);

    /**
     * Última solicitud del usuario (de cualquier estado), útil para exponer
     * el estado actual en el frontend.
     */
    Optional<SolicitudEliminacionCuenta> findTopByUsuarioIdOrderByFechaSolicitudDesc(ObjectId usuarioId);

    /**
     * Solicitudes programadas cuyo plazo de recuperación de 30 días ya se
     * cumplió. Las usa el job automático para anonimizar los datos.
     */
    List<SolicitudEliminacionCuenta> findByEstadoAndFechaProgramadaEliminacionBefore(
            EstadoSolicitudEliminacion estado, LocalDateTime fecha);
}

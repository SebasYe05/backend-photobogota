package com.photobogota.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.photobogota.api.model.Canje;

public interface CanjeRepository extends MongoRepository<Canje, String> {

    Optional<Canje> findByCodigoIgnoreCase(String codigo);

    long countByMiembroNombre(String miembroNombre);

    List<Canje> findByMiembroNombre(String miembroNombre);

    List<Canje> findByPromocionId(String promocionId);

    List<Canje> findByPromocionIdAndMiembroNombre(String promocionId, String miembroNombre);

    // Canjes realizados por el socio en un rango de fechas (para estadísticas).
    List<Canje> findBySocioUsernameAndFechaCanjeBetween(String socioUsername,
            java.time.LocalDateTime desde, java.time.LocalDateTime hasta);
}
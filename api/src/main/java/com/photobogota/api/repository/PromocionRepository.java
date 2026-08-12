package com.photobogota.api.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.photobogota.api.model.Promocion;

public interface PromocionRepository extends MongoRepository<Promocion, String> {

    List<Promocion> findBySocioUsername(String socioUsername);

    List<Promocion> findBySpotId(String spotId);

    // Éstas son las promociones visibles al público: activas y dentro del rango
    // de fechas. La exclusión por fecha se hace en el servicio (LocalDateTime).
    List<Promocion> findByActivoTrue();

    List<Promocion> findBySpotIdIn(Collection<String> spotIds);
}
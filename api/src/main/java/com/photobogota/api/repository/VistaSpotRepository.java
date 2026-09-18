package com.photobogota.api.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.photobogota.api.model.VistaSpot;

public interface VistaSpotRepository extends MongoRepository<VistaSpot, String> {

    List<VistaSpot> findBySpotIdInAndFechaBetween(Collection<String> spotIds, LocalDateTime desde, LocalDateTime hasta);

    // Deduplicación: se evita registrar vistas repetidas del mismo visitante
    // dentro de la última hora (un mismo usuario abriendo el detalle varias veces).
    boolean existsBySpotIdAndUsuarioAndFechaAfter(String spotId, String usuario, LocalDateTime fecha);

    boolean existsBySpotIdAndUsuarioIsNullAndFechaAfter(String spotId, LocalDateTime fecha);
}
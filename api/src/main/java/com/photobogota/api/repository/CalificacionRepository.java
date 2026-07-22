package com.photobogota.api.repository;

import com.photobogota.api.model.Calificacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CalificacionRepository extends MongoRepository<Calificacion, String> {

    List<Calificacion> findBySpotId(String spotId);
}

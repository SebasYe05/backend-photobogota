package com.photobogota.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.photobogota.api.model.PreferenciasNotificacion;

public interface PreferenciasNotificacionRepository extends MongoRepository<PreferenciasNotificacion, String> {

    Optional<PreferenciasNotificacion> findByUsername(String username);

    // Usado para encontrar a quién avisar cuando se publica un spot nuevo:
    // cualquier usuario que tenga esa localidad o esa categoría entre sus intereses.
    List<PreferenciasNotificacion> findByLocalidadesInteresContainingOrCategoriasInteresContaining(
            String localidad, String categoria);
}

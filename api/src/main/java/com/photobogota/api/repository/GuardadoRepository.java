package com.photobogota.api.repository;

import com.photobogota.api.model.Guardado;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface GuardadoRepository extends MongoRepository<Guardado, String> {

    List<Guardado> findByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuarioAndSpotId(String nombreUsuario, String spotId);

    long countByNombreUsuario(String nombreUsuario);
}
package com.photobogota.api.repository;

import com.photobogota.api.model.Guardado;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface GuardadoRepository extends MongoRepository<Guardado, String> {

    List<Guardado> findByUsuario(String usuario);

    boolean existsByUsuarioAndSpotId(String usuario, String spotId);

    long countByUsuario(String usuario);
}
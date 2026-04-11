package com.photobogota.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.photobogota.api.model.Localidades;

public interface LocalidadesRepository extends MongoRepository<Localidades, String> {

    List<Localidades> findByActivoTrue();

    Optional<Localidades> findByNombreAndActivoTrue(String nombre);

    boolean existsByNombre(String nombre);
}
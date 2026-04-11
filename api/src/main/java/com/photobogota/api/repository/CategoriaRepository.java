package com.photobogota.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.photobogota.api.model.Categoria;

public interface CategoriaRepository extends MongoRepository<Categoria, String> {

    List<Categoria> findByActivoTrue();

    Optional<Categoria> findByNombreAndActivoTrue(String nombre);

    boolean existsByNombre(String nombre);
}
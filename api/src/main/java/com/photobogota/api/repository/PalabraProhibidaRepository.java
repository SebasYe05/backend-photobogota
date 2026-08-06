package com.photobogota.api.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.photobogota.api.model.PalabraProhibida;

public interface PalabraProhibidaRepository extends MongoRepository<PalabraProhibida, String> {

    List<PalabraProhibida> findByActivoTrue();

    List<PalabraProhibida> findAllByOrderByFechaCreacionDesc();
}

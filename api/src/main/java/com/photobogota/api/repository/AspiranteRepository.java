package com.photobogota.api.repository;

import com.photobogota.api.model.Aspirante;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface AspiranteRepository extends MongoRepository<Aspirante, String> {

    Optional<Aspirante> findByEmail(String email);

    Optional<Aspirante> findByNit(String nit);

    Optional<Aspirante> findByEmailOrNit(String email, String nit);
}
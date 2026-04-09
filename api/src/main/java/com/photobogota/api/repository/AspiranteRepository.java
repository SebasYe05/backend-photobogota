package com.photobogota.api.repository;

import com.photobogota.api.model.Aspirante;
import com.photobogota.api.model.EstadoAspirante;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface AspiranteRepository extends MongoRepository<Aspirante, String> {

    Optional<Aspirante> findByEmail(String email);

    Optional<Aspirante> findByNit(String nit);

    Optional<Aspirante> findByEmailOrNit(String email, String nit);

    Optional<Aspirante> findByCodigo(String codigo);

    List<Aspirante> findByEstado(EstadoAspirante estado);
}
package com.photobogota.api.repository;

import com.photobogota.api.model.Spot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface SpotRepository extends MongoRepository<Spot, String> {

    List<Spot> findByCategoria(String categoria);

    List<Spot> findByLocalidad(String localidad);

    List<Spot> findByCategoriaAndLocalidad(String categoria, String localidad);

    @Query("{ 'nombre': { $regex: ?0, $options: 'i' } }")
    List<Spot> findByNombreContainingIgnoreCase(String nombre);
}
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

    /**
     * Spots creados por un usuario específico (SOCIO/MODERADOR). Se usa al
     * procesar la eliminación de una cuenta para detectar dependencias y
     * anonimizar al creador sin borrar el spot ni sus estadísticas.
     */
    List<Spot> findByCreadorUsername(String creadorUsername);
}
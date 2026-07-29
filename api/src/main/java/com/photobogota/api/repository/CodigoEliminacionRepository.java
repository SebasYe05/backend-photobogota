package com.photobogota.api.repository;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.photobogota.api.model.CodigoEliminacionCuenta;

/**
 * Repository para manejar los códigos de verificación de eliminación de
 * cuenta.
 */
public interface CodigoEliminacionRepository extends MongoRepository<CodigoEliminacionCuenta, ObjectId> {

    /**
     * Busca un código de eliminación por email y código.
     *
     * @param email  el email del usuario
     * @param codigo el código de verificación
     * @return Optional con el código si existe
     */
    Optional<CodigoEliminacionCuenta> findByEmailAndCodigo(String email, String codigo);

    /**
     * Elimina todos los códigos de eliminación asociados a un email.
     *
     * @param email el email del usuario
     */
    void deleteByEmail(String email);
}

package com.photobogota.api.repository;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.photobogota.api.model.CodigoRecuperacion;

/**
 * Repository para manejar los códigos de recuperación de contraseña.
 */
@Repository
public interface CodigoRecuperacionRepository extends MongoRepository<CodigoRecuperacion, ObjectId> {
    
    /**
     * Busca un código de recuperación por email que no haya sido usado.
     * 
     * @param email el email del usuario
     * @return Optional con el código de recuperación si existe y no ha sido usado
     */
    Optional<CodigoRecuperacion> findTopByEmailAndUsadoFalseOrderByFechaCreacionDesc(String email);
    
    /**
     * Busca un código de recuperación por email y código.
     * 
     * @param email el email del usuario
     * @param codigo el código de recuperación
     * @return Optional con el código de recuperación si existe
     */
    Optional<CodigoRecuperacion> findByEmailAndCodigo(String email, String codigo);
    
    /**
     * Elimina todos los códigos de recuperación asociados a un email.
     * 
     * @param email el email del usuario
     */
    void deleteByEmail(String email);
}
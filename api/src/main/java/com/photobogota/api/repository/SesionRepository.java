package com.photobogota.api.repository;

import com.photobogota.api.model.Sesion;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SesionRepository extends MongoRepository<Sesion, ObjectId> {
    Optional<Sesion> findByRefreshTokenAndActivoTrue(String refreshToken);
    void deleteByRefreshToken(String refreshToken);
    void deleteByUsuarioId(String usuarioId);
}
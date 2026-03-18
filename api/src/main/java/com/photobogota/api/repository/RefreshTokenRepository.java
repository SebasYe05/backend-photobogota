package com.photobogota.api.repository;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.photobogota.api.model.RefreshToken;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, ObjectId> {
    Optional<RefreshToken> findByToken(String token);

    void deleteByEmail(String email); // útil al cambiar contraseña
    
    List<RefreshToken> findAllByEmail(String email);
}

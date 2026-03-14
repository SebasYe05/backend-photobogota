package com.photobogota.api.repository;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.photobogota.api.model.UsuarioAuth;

@Repository
public interface UsuarioAuthRepository extends MongoRepository<UsuarioAuth, ObjectId> {
    
    Optional<UsuarioAuth> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    Optional<UsuarioAuth> findByNombreUsuario(String nombreUsuario);
    
    boolean existsByNombreUsuario(String nombreUsuario);
}

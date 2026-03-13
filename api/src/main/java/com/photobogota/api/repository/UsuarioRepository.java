package com.photobogota.api.repository;


import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.photobogota.api.model.Usuario;

@Repository
public interface UsuarioRepository extends MongoRepository<Usuario, ObjectId> {
    
    Optional<Usuario> findByEmail(String email);
    
    boolean existsByEmail(String email);
}

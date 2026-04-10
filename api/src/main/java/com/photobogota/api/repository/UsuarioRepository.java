package com.photobogota.api.repository;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.photobogota.api.model.Usuario;

@Repository
public interface UsuarioRepository extends MongoRepository<Usuario, ObjectId> {

    Optional<Usuario> findById(ObjectId id);

    Page<Usuario> findByEstadoCuenta(Boolean estadoCuenta, Pageable pageable);

    @Query("{ 'nombresCompletos': { $regex: ?0, $options: 'i' } }")
    Page<Usuario> findByNombresCompletosContainingIgnoreCase(String nombres, Pageable pageable);
}

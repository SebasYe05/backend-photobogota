package com.photobogota.api.repository;


import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.photobogota.api.model.Miembro;

@Repository
public interface MiembroRepository extends MongoRepository<Miembro, ObjectId> {
    
}

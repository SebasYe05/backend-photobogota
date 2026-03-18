package com.photobogota.api.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.photobogota.api.model.Admin;

@Repository
public interface AdminRepository extends MongoRepository<Admin, ObjectId> {
    
    
}

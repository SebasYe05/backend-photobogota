package com.photobogota.api.repository;

import com.photobogota.api.model.PuntosConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PuntosConfigRepository extends MongoRepository<PuntosConfig, String> {
}

package com.photobogota.api.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RefreshTokenIndexConfig {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void createRefreshTokenTTLIndex() {
        // Crear índice TTL en fechaExpiracion para auto-eliminar tokens vencidos
        mongoTemplate.indexOps("refresh-tokens")
            .createIndex(new Index()
                .on("fechaExpiracion", org.springframework.data.domain.Sort.Direction.ASC)
                .expire(0, TimeUnit.SECONDS)
                .named("fechaExpiracion_ttl"));
    }
}

package com.photobogota.api.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component("mongoAtlasHealth") 
public class MongoAtlasHealthIndicator implements HealthIndicator {

    private final MongoTemplate mongoTemplate;

    public MongoAtlasHealthIndicator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Health health() {
        try {
            // Atlas permite el comando 'ping' en nuestra base de datos
            mongoTemplate.executeCommand("{ ping: 1 }");
            return Health.up()
                         .withDetail("database", "photobogota-demo")
                         .withDetail("status", "Connected to Atlas")
                         .build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}

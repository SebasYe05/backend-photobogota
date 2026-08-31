package com.photobogota.api.config;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoAtlasHealthIndicatorTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Test
    void health_cuandoMongoResponde_devuelveUpConDetalles() {
        when(mongoTemplate.executeCommand("{ ping: 1 }")).thenReturn(new Document("ok", 1.0));

        MongoAtlasHealthIndicator indicador = new MongoAtlasHealthIndicator(mongoTemplate);
        Health health = indicador.health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails()).containsEntry("database", "photobogota-demo")
                .containsEntry("status", "Connected to Atlas");
    }

    @Test
    void health_cuandoMongoFalla_devuelveDown() {
        when(mongoTemplate.executeCommand("{ ping: 1 }")).thenThrow(new RuntimeException("sin conexión"));

        MongoAtlasHealthIndicator indicador = new MongoAtlasHealthIndicator(mongoTemplate);
        Health health = indicador.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
    }
}
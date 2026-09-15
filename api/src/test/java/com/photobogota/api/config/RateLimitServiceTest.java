package com.photobogota.api.config;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.bucket4j.Bucket;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitServiceTest {

    private final RateLimitService rateLimitService = new RateLimitService();

    @Test
    void tryConsume_authSensible_permite10PeticionesYLuegoRebota() {
        String ip = "192.168.1.1";

        for (int i = 1; i <= 10; i++) {
            assertThat(rateLimitService.tryConsume(ip, "/api/v1/auth/login")).isTrue();
        }
        assertThat(rateLimitService.tryConsume(ip, "/api/v1/auth/login")).isFalse();
    }

    @Test
    void tryConsume_general_permite60PeticionesYLuegoRebota() {
        String ip = "192.168.1.2";

        for (int i = 1; i <= 60; i++) {
            assertThat(rateLimitService.tryConsume(ip, "/api/v1/spots")).isTrue();
        }
        assertThat(rateLimitService.tryConsume(ip, "/api/v1/spots")).isFalse();
    }

    @Test
    void tryConsume_clasificaLasRutasDeContrasenaComoSensibles() {
        String ip = "192.168.1.3";
        String uri = "/api/v1/passwords/recuperar";

        for (int i = 1; i <= 10; i++) {
            assertThat(rateLimitService.tryConsume(ip, uri)).isTrue();
        }
        assertThat(rateLimitService.tryConsume(ip, uri)).isFalse();
    }

    @Test
    void buckets_sonIndependientesPorIP() {
        String ipA = "10.0.0.1";
        String ipB = "10.0.0.2";

        for (int i = 1; i <= 10; i++) {
            rateLimitService.tryConsume(ipA, "/api/v1/auth/register");
        }

        assertThat(rateLimitService.tryConsume(ipA, "/api/v1/auth/register")).isFalse();
        assertThat(rateLimitService.tryConsume(ipB, "/api/v1/auth/register")).isTrue();
        assertThat(rateLimitService.tryConsume(ipB, "/api/v1/auth/register")).isTrue();
    }

    @Test
    void obtenerEstadisticas_reflejaLosBucketsRegistrados() {
        rateLimitService.tryConsume("10.1.1.1", "/api/v1/auth/login");
        rateLimitService.tryConsume("10.1.1.1", "/api/v1/spots");
        rateLimitService.tryConsume("10.1.1.2", "/api/v1/auth/refresh");

        Map<String, Object> stats = rateLimitService.obtenerEstadisticas();

        assertThat(stats.get("totalIPs")).isEqualTo(3);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> buckets = (List<Map<String, Object>>) stats.get("buckets");
        assertThat(buckets).hasSize(3);
        assertThat(buckets).anySatisfy(bucket -> assertThat(bucket.get("clave"))
                .isIn("10.1.1.1:auth_sensible", "10.1.1.2:auth_sensible", "10.1.1.1:general"));
    }

    @Test
    void resolveBucket_devuelveElMismoBucketParaLaMismaClave() {
        Bucket primero = rateLimitService.resolveBucket("172.16.0.1", "/api/v1/auth/login");
        Bucket segundo = rateLimitService.resolveBucket("172.16.0.1", "/api/v1/auth/login");

        assertThat(segundo).isSameAs(primero);
    }
}
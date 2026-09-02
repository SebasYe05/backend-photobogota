package com.photobogota.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

class RateLimitFilterTest {

    private final RateLimitService rateLimitService = new RateLimitService();
    private final RateLimitFilter filter = new RateLimitFilter(rateLimitService);

    @Test
    void peticionOptions_pasaSinConsumirTokens() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(200);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Remaining")).isNull();
    }

    @Test
    void dentroDelLimite_continuaLaCadenaYAgregaHeaderInformativo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/spots");
        request.setRemoteAddr("10.0.0.9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(200);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("59");
    }

    @Test
    void alExcederElLimite_devuelve429ConRetryAfterYBodyJson() throws Exception {
        String ip = "10.0.0.10";
        for (int i = 0; i < 60; i++) {
            rateLimitService.tryConsume(ip, "/api/v1/spots");
        }

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/spots");
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mockChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotNull();
        assertThat(response.getContentAsString()).contains("Too Many Requests");
        org.mockito.Mockito.verifyNoInteractions(chain);
    }

    @Test
    void rutasDeAuthDifierenciadasConMenorLimite() throws Exception {
        String ip = "10.0.0.11";
        for (int i = 0; i < 10; i++) {
            rateLimitService.tryConsume(ip, "/api/v1/auth/login");
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mockChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void resuelveIPApartirdelHeaderDePruebaXTestClientIP() throws Exception {
        String ip = "20.0.0.1";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/spots");
        request.setRemoteAddr("otra-ip");
        request.addHeader("X-Test-Client-IP", ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mockChain();

        filter.doFilter(request, response, chain);

        // Consume 1 token del bucket de "20.0.0.1": quedan 59.
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("59");

        MockHttpServletResponse segunda = new MockHttpServletResponse();
        filter.doFilter(request, segunda, chain);

        // La segunda petición desde la misma IP consume otro token del mismo bucket.
        assertThat(segunda.getHeader("X-RateLimit-Remaining")).isEqualTo("58");
    }

    private FilterChain mockChain() throws ServletException, IOException {
        return org.mockito.Mockito.mock(FilterChain.class);
    }
}
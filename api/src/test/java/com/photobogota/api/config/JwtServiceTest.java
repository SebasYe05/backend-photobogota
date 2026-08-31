package com.photobogota.api.config;

import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRETO =
            "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWp3dC1zZWNyZXQta2V5LWZvci1wb3RvYm9nb3Rh";

    private final JwtService jwtService = new JwtService();

    @BeforeEach
    void configurarPropiedades() {
        ReflectionTestUtils.setField(jwtService, "claveSecreta", SECRETO);
        ReflectionTestUtils.setField(jwtService, "tiempoExpiracion", 3_600_000L);
        ReflectionTestUtils.setField(jwtService, "tiempoExpiracionRefresh", 604_800_000L);
    }

    private SecretKey claveFirmado() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRETO));
    }

    @Test
    void generarToken_devuelveUnTokenConElNombreDeUsuario() {
        String token = jwtService.generarToken("juan");

        assertThat(token).isNotBlank();
        assertThat(jwtService.extraerNombreUsuario(token)).isEqualTo("juan");
    }

    @Test
    void generarToken_conExtraClaims_incluyeLosClaims() {
        String token = jwtService.generarToken(Map.of("rol", "ADMIN"), "juan");

        String rol = jwtService.extraerClaim(token, claims -> claims.get("rol", String.class));

        assertThat(rol).isEqualTo("ADMIN");
    }

    @Test
    void esTokenValido_conTokenValido_devuelveTrue() {
        String token = jwtService.generarToken("juan");

        assertThat(jwtService.esTokenValido(token, "juan")).isTrue();
    }

    @Test
    void esTokenValido_conNombreDeUsuarioDistinto_devuelveFalse() {
        String token = jwtService.generarToken("juan");

        assertThat(jwtService.esTokenValido(token, "otro")).isFalse();
    }

    @Test
    void esTokenValido_conTokenExpirado_lanzaExpiredJwtException() {
        String tokenExpirado = Jwts.builder()
                .issuedAt(new Date(System.currentTimeMillis() - 7_200_000L))
                .expiration(new Date(System.currentTimeMillis() - 3_600_000L))
                .subject("juan")
                .signWith(claveFirmado())
                .compact();

        assertThatThrownBy(() -> jwtService.esTokenValido(tokenExpirado, "juan"))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void generarTokenRefresh_devuelveUnTokenValidoParaElMismoUsuario() {
        String tokenAcceso = jwtService.generarToken("juan");
        String tokenRefresh = jwtService.generarTokenRefresh("juan");

        assertThat(tokenRefresh).isNotBlank();
        assertThat(tokenRefresh).isNotEqualTo(tokenAcceso);
        assertThat(jwtService.extraerNombreUsuario(tokenRefresh)).isEqualTo("juan");
        assertThat(jwtService.esTokenValido(tokenRefresh, "juan")).isTrue();
    }

    @Test
    void generarToken_cadaTokenEsUnico() {
        String primero = jwtService.generarToken("juan");
        String segundo = jwtService.generarToken("juan");

        assertThat(segundo).isNotEqualTo(primero);
    }
}
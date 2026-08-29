package com.photobogota.api.service;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.photobogota.api.config.JwtService;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.exception.UnauthorizedException;
import com.photobogota.api.model.RefreshToken;
import com.photobogota.api.repository.RefreshTokenRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RefreshTokenImpl refreshTokenService;

    @Test
    void crearRefreshToken_limpiaAnteriorYGuardaNuevo() {
        when(jwtService.generarTokenRefresh("juan@photobogota.com")).thenReturn("token-refresco");
        RefreshToken guardado = RefreshToken.builder()
                .token("token-refresco")
                .email("juan@photobogota.com")
                .fechaExpiracion(new Date(System.currentTimeMillis() + 604800000L))
                .revocado(false)
                .fechaCreacion(new Date())
                .build();
        when(refreshTokenRepository.save(org.mockito.ArgumentMatchers.any(RefreshToken.class))).thenReturn(guardado);

        String resultado = refreshTokenService.crearRefreshToken("juan@photobogota.com");

        assertThat(resultado).isEqualTo("token-refresco");
        verify(refreshTokenRepository).deleteByEmail("juan@photobogota.com");
    }

    @Test
    void obtenerEmailSiValido_tokenValido_devuelveEmail() {
        RefreshToken token = RefreshToken.builder()
                .email("juan@photobogota.com")
                .fechaExpiracion(new Date(System.currentTimeMillis() + 3600000L))
                .revocado(false)
                .build();
        when(refreshTokenRepository.findByToken("token-vigente")).thenReturn(Optional.of(token));

        String email = refreshTokenService.obtenerEmailSiValido("token-vigente");

        assertThat(email).isEqualTo("juan@photobogota.com");
        verify(refreshTokenRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void obtenerEmailSiValido_tokenInexistente_lanzaResourceNotFound() {
        when(refreshTokenRepository.findByToken("token-desconocido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.obtenerEmailSiValido("token-desconocido"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no encontrada en el servidor");
    }

    @Test
    void obtenerEmailSiValido_tokenRevocado_limpiaYlanzaUnauthorized() {
        RefreshToken token = RefreshToken.builder()
                .email("juan@photobogota.com")
                .fechaExpiracion(new Date(System.currentTimeMillis() + 3600000L))
                .revocado(true)
                .build();
        when(refreshTokenRepository.findByToken("token-revocado")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.obtenerEmailSiValido("token-revocado"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("revocada");
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void obtenerEmailSiValido_tokenExpirado_limpiaYlanzaUnauthorized() {
        RefreshToken token = RefreshToken.builder()
                .email("juan@photobogota.com")
                .fechaExpiracion(new Date(System.currentTimeMillis() - 3600000L))
                .revocado(false)
                .build();
        when(refreshTokenRepository.findByToken("token-expirado")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.obtenerEmailSiValido("token-expirado"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expirado");
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void revocarToken_tokenExistente_elimina() {
        RefreshToken token = RefreshToken.builder().token("token").build();
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.of(token));

        refreshTokenService.revocarToken("token");

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void revocarToken_tokenInexistente_noHaceNada() {
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.empty());

        refreshTokenService.revocarToken("token");

        verify(refreshTokenRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void revocarTodosLosTokens_eliminaPorEmail() {
        refreshTokenService.revocarTodosLosTokens("juan@photobogota.com");

        verify(refreshTokenRepository).deleteByEmail("juan@photobogota.com");
    }
}
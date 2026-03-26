package com.photobogota.api.service;

import java.util.Date;

import org.springframework.stereotype.Service;

import com.photobogota.api.config.JwtService;
import com.photobogota.api.exception.UnauthorizedException;
import com.photobogota.api.model.RefreshToken;
import com.photobogota.api.repository.RefreshTokenRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class RefreshTokenImpl implements IRefreshToken {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Override
    public String crearRefreshToken(String email) {

        refreshTokenRepository.deleteByEmail(email);

        String token = jwtService.generarTokenRefresh(email);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .email(email)
                .fechaExpiracion(new Date(System.currentTimeMillis() + 604800000L))
                .revocado(false)
                .fechaCreacion(new Date())
                .build();

        return refreshTokenRepository.save(refreshToken).getToken();
    }

    @Override
    public String obtenerEmailSiValido(String token) {
        // 1. Si el token no existe en Mongo, mandamos 401
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Sesión no encontrada en el servidor"));

        // 2. Si está revocado, limpiamos y mandamos 401
        if (rt.isRevocado()) {
            refreshTokenRepository.delete(rt);
            throw new UnauthorizedException("La sesión ha sido revocada");
        }

        // 3. Si expiró, limpiamos y mandamos 401
        if (rt.getFechaExpiracion().before(new Date())) {
            refreshTokenRepository.delete(rt);
            throw new UnauthorizedException("La sesión ha expirado");
        }

        return rt.getEmail();
    }

    @Override
    public void revocarToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    @Override
    public void revocarTodosLosTokens(String email) {
        refreshTokenRepository.deleteByEmail(email);
    }
}
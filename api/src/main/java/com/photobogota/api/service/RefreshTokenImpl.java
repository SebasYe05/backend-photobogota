package com.photobogota.api.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.photobogota.api.config.JwtService;
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
        // Revocar tokens anteriores del usuario
        revocarTodosLosTokens(email);

        // Generar el token de refresh usando JwtService
        String token = jwtService.generarTokenRefresh(email);

        // Crear el objeto RefreshToken
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .email(email)
                .fechaExpiracion(new Date(System.currentTimeMillis() + 604800000L)) // 7 días
                .revocado(false)
                .fechaCreacion(new Date(System.currentTimeMillis()))
                .build();

        // Guardar y retornar el token
        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        return savedToken.getToken();
    }

    @Override
    public String obtenerEmailSiValido(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token no encontrado"));

        if (rt.isRevocado())
            throw new RuntimeException("Token revocado");
        if (rt.getFechaExpiracion().before(new Date())) {
            refreshTokenRepository.delete(rt);
            throw new RuntimeException("Token expirado");
        }

        return rt.getEmail();
    }

    @Override
    public void revocarToken(String token) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);

        if (refreshTokenOpt.isPresent()) {
            RefreshToken refreshToken = refreshTokenOpt.get();
            refreshToken.setRevocado(true);
            refreshTokenRepository.save(refreshToken);
        }
    }

    @Override
    public void revocarTodosLosTokens(String email) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByEmail(email);

        tokens.forEach(t -> t.setRevocado(true));
        refreshTokenRepository.saveAll(tokens);
    }
}

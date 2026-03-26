package com.photobogota.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitar el refresco de un token JWT usando un refresh token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDTO {

    /**
     * El refresh token recibido durante el login.
     */
    private String refreshToken;
}
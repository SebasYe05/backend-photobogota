package com.photobogota.api.service;

public interface IRefreshToken {

    String crearRefreshToken(String email);

    String obtenerEmailSiValido(String token);

    void revocarToken(String token);

    void revocarTodosLosTokens(String email);
}

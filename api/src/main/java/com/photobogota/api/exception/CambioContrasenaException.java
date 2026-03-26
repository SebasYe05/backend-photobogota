package com.photobogota.api.exception;

/**
 * Excepción lanzada cuando ocurre un error durante el cambio de contraseña.
 */
public class CambioContrasenaException extends RuntimeException {

    public CambioContrasenaException(String message) {
        super(message);
    }
}

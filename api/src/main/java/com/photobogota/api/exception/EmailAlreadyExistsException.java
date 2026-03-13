package com.photobogota.api.exception;

/**
 * Excepción personalizada que se lanza cuando se intenta registrar
 * un usuario con un email que ya existe en el sistema.
 */
public class EmailAlreadyExistsException extends RuntimeException {
    
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
    
    public EmailAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}

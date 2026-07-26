package com.photobogota.api.exception;

/**
 * Se lanza cuando se intenta una operación que no tiene sentido dado el
 * estado actual del recurso, ej: escalar un reporte que ya está escalado,
 * o pasar a un estado no soportado. Se mapea a 400 Bad Request.
 */
public class OperacionInvalidaException extends RuntimeException {

    public OperacionInvalidaException(String message) {
        super(message);
    }
}

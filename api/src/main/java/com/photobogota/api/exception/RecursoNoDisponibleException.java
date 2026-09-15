package com.photobogota.api.exception;

/**
 * Se lanza cuando el estado actual del recurso impide la operación por un
 * conflicto de negocio, por ejemplo una promoción agotada o un canje ya usado.
 * Se mapea a 409 Conflict.
 */
public class RecursoNoDisponibleException extends RuntimeException {

    public RecursoNoDisponibleException(String message) {
        super(message);
    }
}
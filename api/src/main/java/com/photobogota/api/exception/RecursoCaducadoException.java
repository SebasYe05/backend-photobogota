package com.photobogota.api.exception;

/**
 * Se lanza cuando un recurso sigue existiendo pero ya venció/no puede usarse,
 * por ejemplo un código de canje cuya fecha de expiración ya pasó.
 * Se mapea a 410 Gone.
 */
public class RecursoCaducadoException extends RuntimeException {

    public RecursoCaducadoException(String message) {
        super(message);
    }
}
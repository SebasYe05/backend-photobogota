package com.photobogota.api.exception;

/**
 * Excepción base para conflictos de unicidad en el sistema.
 */
public class ResourceAlreadyExistsException extends RuntimeException {

    private final String field;

    public ResourceAlreadyExistsException(String field, String value) {
        super(String.format("El %s '%s' ya está registrado en PhotoBogota", field, value));
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
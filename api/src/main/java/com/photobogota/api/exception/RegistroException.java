package com.photobogota.api.exception;

public class RegistroException extends RuntimeException {

    public RegistroException(String message) {
        super(message);
    }

    public RegistroException(String message, Throwable cause) {
        super(message, cause);
    }
}
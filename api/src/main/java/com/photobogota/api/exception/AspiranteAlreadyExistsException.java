package com.photobogota.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class AspiranteAlreadyExistsException extends RuntimeException {

    public AspiranteAlreadyExistsException(String message) {
        super(message);
    }
}
package com.photobogota.api.exception;

public class EmailAlreadyExistsException extends ResourceAlreadyExistsException {

    public EmailAlreadyExistsException(String email) {
        super("email", email);
    }
}
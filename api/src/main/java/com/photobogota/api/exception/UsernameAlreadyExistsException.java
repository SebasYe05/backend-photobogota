package com.photobogota.api.exception;

public class UsernameAlreadyExistsException extends ResourceAlreadyExistsException {

    public UsernameAlreadyExistsException(String username) {
        super("nombre de usuario", username);
    }
}
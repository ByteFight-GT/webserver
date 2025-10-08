package com.example.botfightwebserver.auth.domain;

public class UsernameAlreadyExistsException extends RegistrationException {
    public UsernameAlreadyExistsException(String username) {
        super("Username is taken: " + username);
    }
}

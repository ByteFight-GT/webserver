package com.example.botfightwebserver.auth.domain;

public class EmailAlreadyRegisteredException extends RegistrationException {
    public EmailAlreadyRegisteredException(String email) {
        super("Email already registered: " + email);
    }
}

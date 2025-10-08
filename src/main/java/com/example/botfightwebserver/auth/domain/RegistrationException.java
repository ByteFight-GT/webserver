package com.example.botfightwebserver.auth.domain;

public abstract class RegistrationException extends RuntimeException {
    public RegistrationException(String message) {
        super(message);
    }
}

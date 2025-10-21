package com.example.botfightwebserver.auth.domain;

public class RegistrationException extends RuntimeException {
    public RegistrationException(String message) {
        super(message);
    }
}
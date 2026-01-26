package org.bytefight.webserver.auth.domain;

public class EmailAlreadyRegisteredException extends RegistrationException {
    public EmailAlreadyRegisteredException(String email) {
        super("Email already registered: " + email);
    }
}
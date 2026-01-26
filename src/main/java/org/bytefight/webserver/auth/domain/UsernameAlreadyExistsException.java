package org.bytefight.webserver.auth.domain;

public class UsernameAlreadyExistsException extends RegistrationException {
    public UsernameAlreadyExistsException(String username) {
        super("Username is taken: " + username);
    }
}
package org.bytefight.webserver.turnstile.domain;

import java.util.List;

public class TurnstileValidationException extends RuntimeException {
  private final transient List<String> errorCodes;

  public TurnstileValidationException(List<String> errorCodes) {
    super("Captcha verification failed.");
    this.errorCodes = errorCodes == null ? List.of() : List.copyOf(errorCodes);
  }

  public List<String> getErrorCodes() {
    return errorCodes;
  }
}

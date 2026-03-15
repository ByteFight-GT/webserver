package org.bytefight.webserver.turnstile.application;

import java.util.List;

public record TurnstileValidationResult(boolean successful, List<String> errorCodes) {

  public static TurnstileValidationResult success() {
    return new TurnstileValidationResult(true, List.of());
  }

  public static TurnstileValidationResult failure(List<String> errorCodes) {
    return new TurnstileValidationResult(false, errorCodes);
  }

  public boolean isFailure() {
    return !successful;
  }
}

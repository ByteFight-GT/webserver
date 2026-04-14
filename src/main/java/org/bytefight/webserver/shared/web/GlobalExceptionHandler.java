package org.bytefight.webserver.shared.web;

import jakarta.validation.ConstraintViolationException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.bytefight.webserver.auth.domain.RegistrationException;
import org.bytefight.webserver.common.domain.PermissionDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  private ProblemDetail problem(HttpStatus status, String title, String detail) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setTitle(title);
    pd.setProperty("timestamp", OffsetDateTime.now().toString());
    return pd;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    log.warn("Validation failed: {}", ex.getMessage());
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Validation Failed");
    pd.setDetail("One or more fields are invalid.");

    Map<String, String> errors = new LinkedHashMap<>();
    for (var e : ex.getBindingResult().getFieldErrors()) {
      errors.put(e.getField(), e.getDefaultMessage());
    }
    pd.setProperty("errors", errors);

    return pd;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
    log.warn("Constraint violation: {}", ex.getMessage());
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Validation failed");

    String detail =
        ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .distinct()
            .collect(java.util.stream.Collectors.joining("; "));
    pd.setDetail(detail);
    return pd;
  }

  @ExceptionHandler(RegistrationException.class)
  ProblemDetail handleRegistration(RegistrationException ex) {
    log.warn("Registration error: {}", ex.getMessage());
    return problem(HttpStatus.CONFLICT, "Registration Error", ex.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("Illegal argument: {}", ex.getMessage());
    return problem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
  }

  @ExceptionHandler(PermissionDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  ProblemDetail handlePermissionDenied(PermissionDeniedException ex) {
    log.warn("Permission denied: {}", ex.getMessage());
    return problem(HttpStatus.CONFLICT, "Permission Denied", ex.getMessage());
  }
}

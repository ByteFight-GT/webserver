package org.bytefight.webserver.turnstile.application;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import org.bytefight.webserver.turnstile.infra.TurnstileProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class TurnstileService {
  private static final String VERIFY_URL =
      "https://challenges.cloudflare.com/turnstile/v0/siteverify";

  private final TurnstileProperties props;
  private final RestClient restClient;

  public TurnstileService(TurnstileProperties props) {
    this.props = props;
    this.restClient = RestClient.builder().build();
  }

  /**
   * Validates a Turnstile token received from the client.
   *
   * @param token the cf-turnstile-response token from the client
   * @param remoteIp optional client IP address for additional validation
   * @return validation result containing success status and any error codes
   */
  public TurnstileValidationResult validate(String token, String remoteIp) {
    if (token == null || token.isBlank()) {
      return TurnstileValidationResult.failure(List.of("missing-input-response"));
    }

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("secret", props.secretKey());
    form.add("response", token);
    if (remoteIp != null && !remoteIp.isBlank()) {
      form.add("remoteip", remoteIp);
    }

    try {
      TurnstileResponse response =
          restClient
              .post()
              .uri(VERIFY_URL)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(TurnstileResponse.class);

      if (response == null) {
        log.warn("Turnstile verification returned null response");
        return TurnstileValidationResult.failure(List.of("invalid-response"));
      }

      if (response.success()) {
        return TurnstileValidationResult.success();
      } else {
        return TurnstileValidationResult.failure(
            response.errorCodes() != null ? response.errorCodes() : List.of("unknown-error"));
      }
    } catch (Exception e) {
      log.error("Turnstile verification failed", e);
      return TurnstileValidationResult.failure(List.of("verification-failed"));
    }
  }

  /** Validates a Turnstile token without providing client IP. */
  public TurnstileValidationResult validate(String token) {
    return validate(token, null);
  }

  private record TurnstileResponse(
      boolean success,
      String challenge_ts,
      String hostname,
      List<String> errorCodes,
      String action,
      String cdata) {

    @SuppressWarnings("unchecked")
    public static TurnstileResponse fromMap(Map<String, Object> map) {
      return new TurnstileResponse(
          Boolean.TRUE.equals(map.get("success")),
          (String) map.get("challenge_ts"),
          (String) map.get("hostname"),
          (List<String>) map.get("error-codes"),
          (String) map.get("action"),
          (String) map.get("cdata"));
    }
  }
}

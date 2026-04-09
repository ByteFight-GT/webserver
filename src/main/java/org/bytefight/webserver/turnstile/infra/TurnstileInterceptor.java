package org.bytefight.webserver.turnstile.infra;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

import org.bytefight.webserver.turnstile.application.TurnstileService;
import org.bytefight.webserver.turnstile.application.TurnstileValidationResult;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class TurnstileInterceptor implements HandlerInterceptor {
  private static final String TURNSTILE_HEADER = "CF-Turnstile-Response";
  private static final String X_FORWARDED_FOR = "X-Forwarded-For";

  private final TurnstileProperties props;
  private final TurnstileService turnstileService;
  private final ObjectMapper objectMapper;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    if (!requiresTurnstile(handlerMethod)) {
      return true;
    }

    if (!props.enabled()) {
      log.debug("Turnstile validation disabled, allowing request");
      return true;
    }

    String token = request.getHeader(TURNSTILE_HEADER);
    String clientIp = resolveClientIp(request);

    TurnstileValidationResult result = turnstileService.validate(token, clientIp);

    if (result.isFailure()) {
      log.warn(
          "Turnstile validation failed for {} {}: {}",
          request.getMethod(),
          request.getRequestURI(),
          result.errorCodes());
      writeErrorResponse(response, result);
      return false;
    }

    return true;
  }

  private boolean requiresTurnstile(HandlerMethod handlerMethod) {
    // Check method-level annotation first
    if (handlerMethod.hasMethodAnnotation(RequireTurnstile.class)) {
      return true;
    }

    // Check class-level annotation
    return handlerMethod.getBeanType().isAnnotationPresent(RequireTurnstile.class);
  }

  private String resolveClientIp(HttpServletRequest request) {
    String xff = request.getHeader(X_FORWARDED_FOR);
    if (xff != null && !xff.isBlank()) {
      // Take the first IP in the chain (original client)
      int comma = xff.indexOf(',');
      return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
    }
    return request.getRemoteAddr();
  }

  private void writeErrorResponse(HttpServletResponse response, TurnstileValidationResult result)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    Map<String, Object> body = Map.of("error", "turnstile_failed", "codes", result.errorCodes());

    objectMapper.writeValue(response.getWriter(), body);
  }
}

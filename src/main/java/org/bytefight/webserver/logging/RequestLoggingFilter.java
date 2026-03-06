package org.bytefight.webserver.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String requestId =
          Optional.ofNullable(request.getHeader("X-Request-ID"))
              .orElse(UUID.randomUUID().toString());

      MDC.put("requestId", requestId);
      MDC.put("httpMethod", request.getMethod());
      MDC.put("httpPath", request.getRequestURI());

      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
        MDC.put("userId", auth.getName());
      }

      response.setHeader("X-Request-ID", requestId);
      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }
}

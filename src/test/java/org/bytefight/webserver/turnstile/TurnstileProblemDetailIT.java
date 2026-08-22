package org.bytefight.webserver.turnstile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.turnstile.application.TurnstileService;
import org.bytefight.webserver.turnstile.application.TurnstileValidationResult;
import org.bytefight.webserver.user.application.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * A failed captcha must come back as a {@code ProblemDetail}, like every other API error.
 *
 * <p>The interception point is what makes this worth an integration test: {@code
 * TurnstileInterceptor} throws from {@code preHandle}, before the handler runs, and the response is
 * only shaped correctly if the {@code DispatcherServlet} still routes that through {@code
 * GlobalExceptionHandler}. A unit test of either class in isolation would not catch a regression
 * here.
 */
@Transactional
@TestPropertySource(properties = "turnstile.enabled=true")
class TurnstileProblemDetailIT extends FullStackIntegrationTestBase {

  private static final String SIGNUP_BODY =
      """
      {"email":"burdell@gatech.edu","password":"hunter2-correct-horse","name":"burdell"}
      """;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TurnstileService turnstileService;

  @MockitoBean private UserService userService;

  @Test
  void aFailedCaptchaReturnsAProblemDetail() throws Exception {
    when(turnstileService.validate(any(), any()))
        .thenReturn(TurnstileValidationResult.failure(List.of("invalid-input-response")));

    mockMvc
        .perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .header("CF-Turnstile-Response", "a-bad-token")
                .content(SIGNUP_BODY))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.title").value("Turnstile Verification Failed"))
        .andExpect(jsonPath("$.detail").value("Captcha verification failed."))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.codes[0]").value("invalid-input-response"));
  }

  @Test
  void aFailedCaptchaNeverReachesTheHandler() throws Exception {
    when(turnstileService.validate(any(), any()))
        .thenReturn(TurnstileValidationResult.failure(List.of("missing-input-response")));

    mockMvc
        .perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SIGNUP_BODY))
        .andExpect(status().isForbidden());

    verify(userService, never()).signup(any());
  }

  @Test
  void aPassingCaptchaReachesTheHandler() throws Exception {
    when(turnstileService.validate(any(), any())).thenReturn(TurnstileValidationResult.success());

    mockMvc
        .perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .header("CF-Turnstile-Response", "a-good-token")
                .content(SIGNUP_BODY))
        .andExpect(status().isOk());

    verify(userService).signup(any());
  }
}

package org.bytefight.webserver.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.auth.application.AuthService;
import org.bytefight.webserver.auth.domain.RegistrationException;
import org.bytefight.webserver.auth.domain.dto.SupabaseDtos;
import org.bytefight.webserver.user.application.UserService;
import org.bytefight.webserver.user.domain.dto.RegisterUserDto;
import org.bytefight.webserver.user.infra.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end behaviour of the {@code app.signup.allowed-domains} gate on {@link
 * UserService#signup}.
 *
 * <p>The allow-list is pinned here rather than inherited from {@code application.yml} so the test
 * keeps asserting the same thing when the deployed list changes.
 *
 * <p>{@link AuthService} is mocked because the real one posts to Supabase over HTTP; the reject
 * cases would pass without it, but only by accident, and the accept case needs it.
 */
@Transactional
@TestPropertySource(properties = "app.signup.allowed-domains=gatech.edu")
class SignupDomainRestrictionIT extends FullStackIntegrationTestBase {

  @Autowired private UserService userService;

  @Autowired private UserRepository userRepository;

  @MockitoBean private AuthService authService;

  @BeforeEach
  void stubAuthProvider() {
    when(authService.createUser(anyString(), anyString(), anyBoolean(), any(), any()))
        .thenAnswer(
            invocation ->
                new SupabaseDtos.SupabaseUser(
                    UUID.randomUUID().toString(),
                    invocation.getArgument(0),
                    Instant.now(),
                    Map.of(),
                    Map.of()));
  }

  private static RegisterUserDto dto(String email, String username) {
    RegisterUserDto input = new RegisterUserDto();
    input.setEmail(email);
    input.setPassword("hunter2-correct-horse");
    input.setName(username);
    return input;
  }

  @Test
  void acceptsAnAllowedDomain() {
    assertThatCode(() -> userService.signup(dto("burdell@gatech.edu", "burdell")))
        .doesNotThrowAnyException();

    assertThat(userRepository.existsByEmailIgnoreCase("burdell@gatech.edu")).isTrue();
  }

  @Test
  void acceptsASubdomainOfAnAllowedDomain() {
    assertThatCode(() -> userService.signup(dto("burdell@cc.gatech.edu", "burdellcc")))
        .doesNotThrowAnyException();

    assertThat(userRepository.existsByEmailIgnoreCase("burdell@cc.gatech.edu")).isTrue();
  }

  @Test
  void rejectsAnUnlistedDomain() {
    assertThatThrownBy(() -> userService.signup(dto("burdell@example.com", "burdellext")))
        .isInstanceOf(RegistrationException.class)
        .hasMessageContaining("isn't available for your institution");
  }

  @Test
  void rejectsALookalikeDomain() {
    assertThatThrownBy(() -> userService.signup(dto("burdell@notgatech.edu", "burdellfake")))
        .isInstanceOf(RegistrationException.class);
  }

  @Test
  void rejectionLeavesNoUserBehind() {
    assertThatThrownBy(() -> userService.signup(dto("burdell@example.com", "burdellorphan")))
        .isInstanceOf(RegistrationException.class);

    assertThat(userRepository.existsByEmailIgnoreCase("burdell@example.com")).isFalse();
  }

  @Test
  void rejectionNeverReachesTheAuthProvider() {
    // The gate has to run before the Supabase call, otherwise a rejected signup still creates an
    // account there that this service knows nothing about.
    assertThatThrownBy(() -> userService.signup(dto("burdell@example.com", "burdellnocall")))
        .isInstanceOf(RegistrationException.class);

    verify(authService, never()).createUser(anyString(), anyString(), anyBoolean(), any(), any());
  }

  @Test
  void rejectsAnUppercaseAddressOnAnUnlistedDomain() {
    assertThatThrownBy(() -> userService.signup(dto("Burdell@Example.COM", "burdellupper")))
        .isInstanceOf(RegistrationException.class);
  }
}

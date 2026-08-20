package org.bytefight.webserver.auth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.auth.application.JwtToUserAuthenticationConverter;
import org.bytefight.webserver.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

@Transactional
class JwtToUserAuthenticationConverterIT extends FullStackIntegrationTestBase {
  @Autowired private TestDataFactory testDataFactory;

  @Autowired private JwtToUserAuthenticationConverter converter;

  @Test
  void serviceAccountIsGrantedServiceAccountRole() {
    User serviceAccount = testDataFactory.createServiceAccount("engine@example.com");

    assertThat(authoritiesFor(serviceAccount.getUuid()))
        .containsExactlyInAnyOrder("ROLE_USER", "ROLE_SERVICE_ACCOUNT");
  }

  @Test
  void regularUserIsNotGrantedAdminOrServiceAccountRole() {
    User user = testDataFactory.createUser();

    assertThat(authoritiesFor(user.getUuid())).containsExactly("ROLE_USER");
  }

  @Test
  void adminIsGrantedAdminRole() {
    User admin = testDataFactory.createUser("admin@example.com", true);

    assertThat(authoritiesFor(admin.getUuid()))
        .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
  }

  @Test
  void unknownSubjectGetsNoAuthorities() {
    assertThat(authoritiesFor(UUID.randomUUID())).isEmpty();
  }

  private List<String> authoritiesFor(UUID subject) {
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(subject.toString()).build();

    return converter.convert(jwt).getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();
  }
}

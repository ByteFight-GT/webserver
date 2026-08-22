package org.bytefight.webserver.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.bytefight.webserver.auth.infra.SignupProperties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Rules for the {@code app.signup.allowed-domains} gate.
 *
 * <p>Deliberately a plain unit test: the matching is pure string logic and needs no database or
 * container. The lookalike cases below are the point of the test — a naive {@code endsWith} check
 * would let {@code notgatech.edu} register.
 */
class SignupPropertiesTest {

  private static SignupProperties of(String... domains) {
    return new SignupProperties(List.of(domains));
  }

  @Nested
  class Allows {

    @Test
    void theDomainItself() {
      assertThat(of("gatech.edu").isEmailAllowed("burdell@gatech.edu")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"burdell@cc.gatech.edu", "burdell@mail.gatech.edu"})
    void subdomainsOfTheDomain(String email) {
      assertThat(of("gatech.edu").isEmailAllowed(email)).isTrue();
    }

    @Test
    void anyDomainWhenNoneAreConfigured() {
      assertThat(of().isEmailAllowed("burdell@example.com")).isTrue();
    }

    @Test
    void anyDomainWhenThePropertyIsAbsent() {
      assertThat(new SignupProperties(null).isEmailAllowed("burdell@example.com")).isTrue();
    }

    @Test
    void anyOfSeveralConfiguredDomains() {
      SignupProperties properties = of("gatech.edu", "example.com");

      assertThat(properties.isEmailAllowed("burdell@gatech.edu")).isTrue();
      assertThat(properties.isEmailAllowed("burdell@example.com")).isTrue();
    }
  }

  @Nested
  class Rejects {

    @Test
    void anUnlistedDomain() {
      assertThat(of("gatech.edu").isEmailAllowed("burdell@example.com")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "burdell@notgatech.edu",
          "burdell@gatech.edu.evil.com",
          "burdell@evilgatech.edu",
          "burdell@gatech.education"
        })
    void lookalikeDomains(String email) {
      assertThat(of("gatech.edu").isEmailAllowed(email)).isFalse();
    }

    @Test
    void aDomainThatOnlyMatchesTheLocalPart() {
      assertThat(of("gatech.edu").isEmailAllowed("gatech.edu@example.com")).isFalse();
    }

    @Test
    void anAddressWithNoDomain() {
      assertThat(of("gatech.edu").isEmailAllowed("burdell")).isFalse();
      assertThat(of("gatech.edu").isEmailAllowed("burdell@")).isFalse();
    }

    @Test
    void aNullAddress() {
      assertThat(of("gatech.edu").isEmailAllowed(null)).isFalse();
    }
  }

  @Nested
  class Normalizes {

    @Test
    void theCaseOfTheAddress() {
      assertThat(of("gatech.edu").isEmailAllowed("Burdell@GaTech.EDU")).isTrue();
    }

    @Test
    void theCaseAndPaddingOfTheConfiguredDomain() {
      assertThat(of("  GaTech.EDU  ").isEmailAllowed("burdell@gatech.edu")).isTrue();
    }

    @Test
    void aFullyQualifiedTrailingDot() {
      assertThat(of("gatech.edu").isEmailAllowed("burdell@gatech.edu.")).isTrue();
    }

    @Test
    void anAddressWithSeveralAtSigns() {
      // The domain is what follows the LAST '@' — "a@b" is a legal quoted local part.
      assertThat(of("gatech.edu").isEmailAllowed("\"a@example.com\"@gatech.edu")).isTrue();
      assertThat(of("gatech.edu").isEmailAllowed("\"a@gatech.edu\"@example.com")).isFalse();
    }
  }

  @Test
  void toleratesANullEntryInTheConfiguredList() {
    // Binding "- " from YAML yields a null element; it must not blow up the whole gate.
    assertThat(
            new SignupProperties(Arrays.asList("gatech.edu", null)).isEmailAllowed("b@gatech.edu"))
        .isTrue();
  }
}

package org.bytefight.webserver.auth.infra;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Signup gating rules.
 *
 * <p>{@code allowedDomains} restricts which email domains may register. An entry matches the domain
 * itself and any subdomain of it, so {@code gatech.edu} accepts both {@code @gatech.edu} and {@code
 * @cc.gatech.edu} while still rejecting {@code @notgatech.edu}.
 *
 * <p>An empty or absent list means "no restriction configured" and allows every domain, so a
 * missing property degrades to today's behaviour rather than locking everyone out.
 */
@ConfigurationProperties(prefix = "app.signup")
public record SignupProperties(List<String> allowedDomains) {
  public SignupProperties {
    allowedDomains =
        allowedDomains == null
            ? List.of()
            : allowedDomains.stream()
                .filter(domain -> domain != null && !domain.isBlank())
                .map(SignupProperties::normalizeDomain)
                .toList();
  }

  public boolean isEmailAllowed(String email) {
    if (allowedDomains.isEmpty()) {
      return true;
    }

    String domain = domainOf(email);
    if (domain == null || domain.isEmpty()) {
      return false;
    }

    return allowedDomains.stream()
        .anyMatch(allowed -> domain.equals(allowed) || domain.endsWith("." + allowed));
  }

  /** Returns the lower-cased domain of {@code email}, or {@code null} if it has no {@code @}. */
  static String domainOf(String email) {
    if (email == null) {
      return null;
    }

    int at = email.lastIndexOf('@');
    return at < 0 ? null : normalizeDomain(email.substring(at + 1));
  }

  private static String normalizeDomain(String domain) {
    String normalized = domain.trim().toLowerCase(Locale.ROOT);

    // A trailing dot is a legal fully-qualified form ("gatech.edu.") that must not slip past an
    // exact comparison against "gatech.edu".
    return normalized.endsWith(".") ? normalized.substring(0, normalized.length() - 1) : normalized;
  }
}

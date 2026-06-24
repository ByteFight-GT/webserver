package org.bytefight.webserver.player.infra;

import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.player.domain.ProfileVisibility;
import org.springframework.data.jpa.domain.Specification;

public final class PlayerProfileSpecification {
  private PlayerProfileSpecification() {}

  public static Specification<Player> isPublicProfile() {
    return (root, query, cb) ->
      cb.equal(root.get("profileVisibility"), ProfileVisibility.PUBLIC);
  }

  public static Specification<Player> usernameContains(String username) {
    return (root, query, cb) -> {
      if (username == null || username.isBlank()) {
        return cb.conjunction();
      }

      return cb.like(
          cb.lower(root.get("username")),
          "%" + username.trim().toLowerCase() + "%");
    };
  }

  public static Specification<Player> majorContains(String major) {
    return (root, query, cb) -> {
      if (major == null || major.isBlank()) {
        return cb.conjunction();
      }

      return cb.like(
          cb.lower(root.get("major")),
          "%" + major.trim().toLowerCase() + "%");
    };
  }

  public static Specification<Player> graduationYearEquals(Integer year) {
    return (root, query, cb) ->
        year == null ? cb.conjunction() : cb.equal(root.get("graduationYear"), year);
  }
}
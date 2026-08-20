package org.bytefight.webserver.competition.application;

import java.util.Set;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.user.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CompetitionAccessGuard {
  private static final Set<String> PRIVILEGED_ROLES = Set.of("ROLE_ADMIN", "ROLE_SERVICE_ACCOUNT");

  public boolean canSeeInternal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }
    if (authentication.getAuthorities().stream()
        .anyMatch(authority -> PRIVILEGED_ROLES.contains(authority.getAuthority()))) {
      return true;
    }
    return authentication.getPrincipal() instanceof User user && user.isAdminOrServiceAccount();
  }

  public boolean canAccess(Competition competition) {
    return competition != null && (!competition.isInternal() || canSeeInternal());
  }

  public void requireAccess(Competition competition) {
    if (!canAccess(competition)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found");
    }
  }
}

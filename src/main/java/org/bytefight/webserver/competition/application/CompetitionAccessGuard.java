package org.bytefight.webserver.competition.application;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.user.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CompetitionAccessGuard {

  public boolean isAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }
    if (authentication.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
      return true;
    }
    return authentication.getPrincipal() instanceof User user && user.isAdmin();
  }

  public boolean canAccess(Competition competition) {
    return competition != null && (!competition.isInternal() || isAdmin());
  }

  public void requireAccess(Competition competition) {
    if (!canAccess(competition)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found");
    }
  }
}

package org.bytefight.webserver.gamematch.infra;

import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class GameMatchSpecifications {
  private GameMatchSpecifications() {}

  public static Specification<GameMatch> fromFilter(Map<String, Object> filter) {
    if (filter == null || filter.isEmpty()) {
      return (root, query, cb) -> cb.conjunction();
    }

    // Parse eagerly so invalid values fail before the query runs.
    Long competitionId = parseLong(filter.get("competitionId"));
    String ladder = parseString(filter.get("ladder"));
    List<MatchStatus> statuses = parseStatuses(filter.get("status"));

    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (competitionId != null) {
        predicates.add(cb.equal(root.get("competition").get("id"), competitionId));
      }

      if (ladder != null) {
        predicates.add(cb.equal(root.get("ladder"), ladder));
      }

      if (!statuses.isEmpty()) {
        predicates.add(root.get("status").in(statuses));
      }

      if (predicates.isEmpty()) {
        return cb.conjunction();
      }

      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static List<MatchStatus> parseStatuses(Object value) {
    if (value == null) {
      return List.of();
    }
    List<MatchStatus> statuses = new ArrayList<>();
    if (value instanceof Collection<?> values) {
      for (Object item : values) {
        MatchStatus parsed = parseStatus(item);
        if (parsed != null) {
          statuses.add(parsed);
        }
      }
      return statuses;
    }
    MatchStatus single = parseStatus(value);
    return single != null ? List.of(single) : List.of();
  }

  private static MatchStatus parseStatus(Object value) {
    String text = parseString(value);
    if (text == null) {
      return null;
    }
    try {
      return MatchStatus.valueOf(text);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid match status: " + text);
    }
  }

  private static String parseString(Object value) {
    if (value instanceof String text && !text.isBlank()) {
      return text.trim();
    }
    return null;
  }

  private static Long parseLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String text && !text.isBlank()) {
      try {
        return Long.parseLong(text.trim());
      } catch (NumberFormatException ex) {
        return null;
      }
    }
    return null;
  }
}

package org.bytefight.webserver.notification.infra;

import jakarta.persistence.criteria.Predicate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bytefight.webserver.notification.domain.Notification;
import org.bytefight.webserver.notification.domain.NotificationPriority;
import org.springframework.data.jpa.domain.Specification;

public final class NotificationSpecifications {
  private NotificationSpecifications() {}

  /** Admin list filter: {@code competitionId}, {@code priority}, {@code q}, {@code id}. */
  public static Specification<Notification> fromFilter(Map<String, Object> filter) {
    return (root, query, cb) -> {
      if (filter == null || filter.isEmpty()) {
        return cb.conjunction();
      }

      List<Predicate> predicates = new ArrayList<>();

      Long competitionId = parseLong(filter.get("competitionId"));
      if (competitionId != null) {
        predicates.add(cb.equal(root.get("competition").get("id"), competitionId));
      }

      NotificationPriority priority = parsePriority(filter.get("priority"));
      if (priority != null) {
        predicates.add(cb.equal(root.get("priority"), priority));
      }

      if (filter.get("q") instanceof String q && !q.isBlank()) {
        String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("body")), pattern)));
      }

      Long id = parseLong(filter.get("id"));
      if (id != null) {
        predicates.add(cb.equal(root.get("id"), id));
      }

      if (predicates.isEmpty()) {
        return cb.conjunction();
      }

      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  /**
   * What a user is allowed to see: site-wide notifications plus those for the given competitions,
   * excluding expired ones.
   */
  public static Specification<Notification> visibleTo(
      Collection<Long> competitionIds, Instant now) {
    return (root, query, cb) -> {
      Predicate audience = cb.isNull(root.get("competition"));
      if (!competitionIds.isEmpty()) {
        audience = cb.or(audience, root.get("competition").get("id").in(competitionIds));
      }
      Predicate notExpired =
          cb.or(cb.isNull(root.get("expireAt")), cb.greaterThan(root.get("expireAt"), now));
      return cb.and(audience, notExpired);
    };
  }

  public static Specification<Notification> createdAfter(Instant instant) {
    return (root, query, cb) -> cb.greaterThan(root.get("createdAt"), instant);
  }

  public static Specification<Notification> hasPriority(NotificationPriority priority) {
    return (root, query, cb) -> cb.equal(root.get("priority"), priority);
  }

  private static NotificationPriority parsePriority(Object value) {
    if (value instanceof String text && !text.isBlank()) {
      try {
        return NotificationPriority.valueOf(text.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ex) {
        return null;
      }
    }
    return null;
  }

  private static Long parseLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String text && !text.isBlank()) {
      try {
        return Long.parseLong(text);
      } catch (NumberFormatException ex) {
        return null;
      }
    }
    return null;
  }
}

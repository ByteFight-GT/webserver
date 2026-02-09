package org.bytefight.webserver.ladder.infra;

import org.bytefight.webserver.ladder.domain.Ladder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LadderSpecifications {
    private LadderSpecifications() {
    }

    public static Specification<Ladder> fromFilter(Map<String, Object> filter) {
        return (root, query, cb) -> {
            if (filter == null || filter.isEmpty()) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            Long competitionId = parseLong(filter.get("competitionId"));
            if (competitionId != null) {
                predicates.add(cb.equal(root.get("competition").get("id"), competitionId));
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

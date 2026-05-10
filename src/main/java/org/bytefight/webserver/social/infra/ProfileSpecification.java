package org.bytefight.webserver.social.infra;

import jakarta.persistence.criteria.Predicate;
import org.bytefight.webserver.social.domain.Profile;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProfileSpecification {
    public static Specification<Profile> fromFilter(String username, String major, Integer year, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("isDeleted"), false));
            if (username != null) predicates.add(cb.equal(root.get("player").get("username"), username));
            if (major != null) predicates.add(cb.equal(root.get("major"), major));
            if (year != null) predicates.add(cb.equal(root.get("year"), year));
            if (keyword != null) predicates.add(cb.like(root.get("description"), "%" + keyword + "%"));

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
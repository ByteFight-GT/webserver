package org.bytefight.webserver.competition.infra;

import org.bytefight.webserver.competition.domain.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {
    Optional<Competition> findBySlug(String slug);
}

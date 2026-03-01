package org.bytefight.webserver.competition.infra;

import java.util.Optional;

import org.bytefight.webserver.competition.domain.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {
  Optional<Competition> findBySlug(String slug);
}

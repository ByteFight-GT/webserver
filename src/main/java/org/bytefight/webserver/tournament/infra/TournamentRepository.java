package org.bytefight.webserver.tournament.infra;

import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.competition.domain.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for tournament metadata (tournament table).
 */
@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    Optional<Tournament> findByUuid(UUID uuid);
    Optional<Tournament> findByUuidAndCompetition(UUID uuid, Competition competition);
    List<Tournament> findByCompetitionOrderByIdDesc(Competition competition);
}

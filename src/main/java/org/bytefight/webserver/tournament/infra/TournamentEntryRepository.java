package org.bytefight.webserver.tournament.infra;

import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.tournament.domain.TournamentEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for tournament participants (tournament_entry table).
 */
@Repository
public interface TournamentEntryRepository extends JpaRepository<TournamentEntry, Long> {
    List<TournamentEntry> findByTournamentOrderBySeed(Tournament tournament);
    long countByTournament(Tournament tournament);
    boolean existsByTournament(Tournament tournament);
}

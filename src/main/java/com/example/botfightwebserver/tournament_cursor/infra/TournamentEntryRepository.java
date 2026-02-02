package com.example.botfightwebserver.tournament_cursor.infra;

import com.example.botfightwebserver.tournament_cursor.domain.Tournament;
import com.example.botfightwebserver.tournament_cursor.domain.TournamentEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for tournament participants (tournament_cursor_entry table).
 */
@Repository
public interface TournamentEntryRepository extends JpaRepository<TournamentEntry, Long> {
    List<TournamentEntry> findByTournamentOrderBySeed(Tournament tournament);
    long countByTournament(Tournament tournament);
    boolean existsByTournament(Tournament tournament);
}

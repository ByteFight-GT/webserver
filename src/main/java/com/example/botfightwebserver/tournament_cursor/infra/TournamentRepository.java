package com.example.botfightwebserver.tournament_cursor.infra;

import com.example.botfightwebserver.tournament_cursor.domain.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for tournament metadata (tournament_cursor table).
 */
@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    Optional<Tournament> findByUuid(UUID uuid);
}

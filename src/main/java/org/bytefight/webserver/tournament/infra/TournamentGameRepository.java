package org.bytefight.webserver.tournament.infra;

import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.tournament.domain.TournamentGame;
import org.bytefight.webserver.tournament.domain.TournamentMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for individual games within a best-of series (tournament_game table).
 *
 * Primary use cases:
 * - Find which TournamentMatch (series) a GameMatch belongs to
 * - List all games in a series for display/tracking
 */
@Repository
public interface TournamentGameRepository extends JpaRepository<TournamentGame, Long> {
    /**
     * Looks up the TournamentGame by the underlying GameMatch.
     * This is the primary entry point when a GameMatch result arrives and
     * we need to find the parent series (TournamentMatch).
     */
    Optional<TournamentGame> findByGameMatch(GameMatch gameMatch);

    /**
     * Returns all games in a series, ordered by game number.
     * Used for DTO construction to show series progress.
     */
    List<TournamentGame> findByTournamentMatchOrderByGameNumberAsc(TournamentMatch tournamentMatch);
}

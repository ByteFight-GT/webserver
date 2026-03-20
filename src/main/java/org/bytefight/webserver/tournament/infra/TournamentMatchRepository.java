package org.bytefight.webserver.tournament.infra;

import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.tournament.domain.TournamentMatch;
import org.bytefight.webserver.tournament.domain.TournamentBracketType;
import org.bytefight.webserver.tournament.domain.TournamentMatchState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for bracket nodes (tournament_match table).
 *
 * Note: the old findByGameMatch method has been removed. Now that each
 * TournamentMatch is a series containing multiple games, the lookup from
 * GameMatch -> TournamentMatch goes through TournamentGameRepository instead:
 *   TournamentGameRepository.findByGameMatch(gameMatch) -> TournamentGame -> getTournamentMatch()
 */
@Repository
public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Long> {
    List<TournamentMatch> findByTournamentOrderByBracketTypeAscRoundNumberAscMatchIndexAsc(Tournament tournament);
    List<TournamentMatch> findByTournamentAndState(Tournament tournament, TournamentMatchState state);
    Optional<TournamentMatch> findByTournamentAndBracketType(Tournament tournament, TournamentBracketType bracketType);
}

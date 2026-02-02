package com.example.botfightwebserver.tournament_cursor.infra;

import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.tournament_cursor.domain.Tournament;
import com.example.botfightwebserver.tournament_cursor.domain.TournamentMatch;
import com.example.botfightwebserver.tournament_cursor.domain.TournamentBracketType;
import com.example.botfightwebserver.tournament_cursor.domain.TournamentMatchState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for bracket nodes (tournament_cursor_match table).
 */
@Repository
public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Long> {
    List<TournamentMatch> findByTournamentOrderByBracketTypeAscRoundNumberAscMatchIndexAsc(Tournament tournament);
    List<TournamentMatch> findByTournamentAndState(Tournament tournament, TournamentMatchState state);
    Optional<TournamentMatch> findByGameMatch(GameMatch gameMatch);
    Optional<TournamentMatch> findByTournamentAndBracketType(Tournament tournament, TournamentBracketType bracketType);
}

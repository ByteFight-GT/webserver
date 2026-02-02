package com.example.botfightwebserver.tournament_cursor.application;

import com.example.botfightwebserver.tournament_cursor.domain.TournamentMatch;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * In-memory representation of the generated bracket.
 * Used to wire match graph relationships before persisting.
 */
@Getter
@RequiredArgsConstructor
public class TournamentBracketGraph {
    private final List<List<TournamentMatch>> winnersRounds;
    private final List<List<TournamentMatch>> losersRounds;
    private final TournamentMatch grandFinal;
    private final TournamentMatch grandFinalReset;
    private final List<TournamentMatch> allMatches;
}

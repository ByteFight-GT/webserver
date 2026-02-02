package org.bytefight.webserver.tournament.application;

import org.bytefight.webserver.tournament.domain.TournamentMatch;
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
    // Ordered winners bracket rounds (round 1..N).
    private final List<List<TournamentMatch>> winnersRounds;
    // Ordered losers bracket rounds (round 1..M).
    private final List<List<TournamentMatch>> losersRounds;
    // Grand final between winners and losers bracket champions.
    private final TournamentMatch grandFinal;
    // Optional reset if winners-bracket champion loses once.
    private final TournamentMatch grandFinalReset;
    // Flat list of all match nodes (useful for persistence).
    private final List<TournamentMatch> allMatches;
}

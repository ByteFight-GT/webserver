package org.bytefight.webserver.tournament;

import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.tournament.application.TournamentBracketBuilder;
import org.bytefight.webserver.tournament.application.TournamentBracketGraph;
import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.tournament.domain.TournamentBracketType;
import org.bytefight.webserver.tournament.domain.TournamentEntry;
import org.bytefight.webserver.tournament.domain.TournamentMatch;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Validates bracket sizing, wiring, and series length assignment
 * for the double-elimination bracket builder.
 */
public class TournamentBracketBuilderTest {

    @Test
    void buildsBracketWithNextPowerOfTwo() {
        TournamentBracketBuilder builder = new TournamentBracketBuilder();
        Tournament tournament = Tournament.builder().name("Test").build();
        List<TournamentEntry> entries = buildEntries(10);

        TournamentBracketGraph graph = builder.buildBracket(tournament, entries);

        assertEquals(16, tournament.getBracketSize());
        assertEquals(8, graph.getWinnersRounds().get(0).size());
        assertEquals(4, graph.getLosersRounds().get(0).size());
    }

    @Test
    void wiresWinnerAndLoserAdvancement() {
        TournamentBracketBuilder builder = new TournamentBracketBuilder();
        Tournament tournament = Tournament.builder().name("Test").build();
        List<TournamentEntry> entries = buildEntries(8);

        TournamentBracketGraph graph = builder.buildBracket(tournament, entries);

        long id = 1;
        for (TournamentMatch match : graph.getAllMatches()) {
            match.setId(id++);
        }

        builder.wireWinnersAdvancement(graph.getWinnersRounds());
        builder.wireLosersAdvancement(graph.getWinnersRounds(), graph.getLosersRounds());
        builder.wireLosersToGrandFinal(graph.getWinnersRounds(), graph.getLosersRounds(), graph.getGrandFinal());
        builder.wireGrandFinalReset(graph.getGrandFinal(), graph.getGrandFinalReset());

        TournamentMatch w1m1 = graph.getWinnersRounds().get(0).get(0);
        TournamentMatch w2m1 = graph.getWinnersRounds().get(1).get(0);
        TournamentMatch l1m1 = graph.getLosersRounds().get(0).get(0);

        assertEquals(w2m1.getId(), w1m1.getNextWinnerMatchId());
        assertEquals(1, w1m1.getNextWinnerSlot());
        assertEquals(l1m1.getId(), w1m1.getNextLoserMatchId());
        assertEquals(1, w1m1.getNextLoserSlot());
        assertNotNull(graph.getGrandFinal().getId());
    }

    /**
     * Verifies that winners/losers bracket matches get Bo5 and grand finals get Bo7.
     */
    @Test
    void assignsCorrectSeriesLengths() {
        TournamentBracketBuilder builder = new TournamentBracketBuilder();
        Tournament tournament = Tournament.builder().name("SeriesTest").build();
        List<TournamentEntry> entries = buildEntries(4);

        TournamentBracketGraph graph = builder.buildBracket(tournament, entries);

        // All winners bracket matches should be Bo5
        for (List<TournamentMatch> round : graph.getWinnersRounds()) {
            for (TournamentMatch match : round) {
                assertEquals(TournamentBracketBuilder.NORMAL_SERIES_LENGTH, match.getSeriesLength(),
                        "Winners bracket match should be Bo" + TournamentBracketBuilder.NORMAL_SERIES_LENGTH);
                assertEquals(0, match.getTeamOneSeriesWins());
                assertEquals(0, match.getTeamTwoSeriesWins());
            }
        }

        // All losers bracket matches should be Bo5
        for (List<TournamentMatch> round : graph.getLosersRounds()) {
            for (TournamentMatch match : round) {
                assertEquals(TournamentBracketBuilder.NORMAL_SERIES_LENGTH, match.getSeriesLength(),
                        "Losers bracket match should be Bo" + TournamentBracketBuilder.NORMAL_SERIES_LENGTH);
            }
        }

        // Grand final should be Bo7
        assertEquals(TournamentBracketBuilder.GRAND_FINAL_SERIES_LENGTH, graph.getGrandFinal().getSeriesLength(),
                "Grand final should be Bo" + TournamentBracketBuilder.GRAND_FINAL_SERIES_LENGTH);
        assertEquals(TournamentBracketType.GRAND_FINAL, graph.getGrandFinal().getBracketType());

        // Grand final reset should also be Bo7
        assertEquals(TournamentBracketBuilder.GRAND_FINAL_SERIES_LENGTH, graph.getGrandFinalReset().getSeriesLength(),
                "Grand final reset should be Bo" + TournamentBracketBuilder.GRAND_FINAL_SERIES_LENGTH);
        assertEquals(TournamentBracketType.GRAND_FINAL_RESET, graph.getGrandFinalReset().getBracketType());
    }

    /**
     * Verifies that series win counters are initialized to zero.
     */
    @Test
    void seriesWinsInitializedToZero() {
        TournamentBracketBuilder builder = new TournamentBracketBuilder();
        Tournament tournament = Tournament.builder().name("ZeroTest").build();
        List<TournamentEntry> entries = buildEntries(8);

        TournamentBracketGraph graph = builder.buildBracket(tournament, entries);

        for (TournamentMatch match : graph.getAllMatches()) {
            assertEquals(0, match.getTeamOneSeriesWins(), "teamOneSeriesWins should start at 0");
            assertEquals(0, match.getTeamTwoSeriesWins(), "teamTwoSeriesWins should start at 0");
        }
    }

    /**
     * Verifies the winsRequired helper on TournamentMatch.
     */
    @Test
    void winsRequiredCalculation() {
        TournamentBracketBuilder builder = new TournamentBracketBuilder();
        Tournament tournament = Tournament.builder().name("WinsReq").build();
        List<TournamentEntry> entries = buildEntries(4);

        TournamentBracketGraph graph = builder.buildBracket(tournament, entries);

        // Bo5: (5+1)/2 = 3 wins required
        TournamentMatch winnersMatch = graph.getWinnersRounds().get(0).get(0);
        assertEquals(3, winnersMatch.getWinsRequired());

        // Bo7: (7+1)/2 = 4 wins required
        assertEquals(4, graph.getGrandFinal().getWinsRequired());
    }

    private static List<TournamentEntry> buildEntries(int count) {
        List<TournamentEntry> entries = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Team team = new Team();
            team.setName("Team " + i);
            team.setUuid(UUID.randomUUID());
            TournamentEntry entry = new TournamentEntry();
            entry.setTeam(team);
            entry.setSeed(i);
            entries.add(entry);
        }
        return entries;
    }
}

package org.bytefight.webserver.tournament;

import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.tournament.application.TournamentBracketBuilder;
import org.bytefight.webserver.tournament.application.TournamentBracketGraph;
import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.tournament.domain.TournamentEntry;
import org.bytefight.webserver.tournament.domain.TournamentMatch;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Validates bracket sizing and wiring for double-elimination builder.
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

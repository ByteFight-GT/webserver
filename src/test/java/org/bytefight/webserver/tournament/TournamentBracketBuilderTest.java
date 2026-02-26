package org.bytefight.webserver.tournament;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.tournament.application.TournamentBracketBuilder;
import org.bytefight.webserver.tournament.application.TournamentBracketGraph;
import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.tournament.domain.TournamentBracketType;
import org.bytefight.webserver.tournament.domain.TournamentEntry;
import org.bytefight.webserver.tournament.domain.TournamentMatch;
import org.bytefight.webserver.tournament.domain.TournamentMatchState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates bracket sizing, wiring, and series length assignment
 * for the double-elimination bracket builder.
 */
public class TournamentBracketBuilderTest extends FullStackIntegrationTestBase {

    @Autowired
    private TournamentBracketBuilder tournamentBracketBuilder;

    /**
     * Validates bracket sizing expands to the next power of two and
     * that round 1 sizes are derived from that expanded size.
     *
     * Steps:
     * 1) Build a bracket with 10 entries (not a power of two).
     * 2) Assert the tournament bracket size is expanded to 16.
     * 3) Assert winners round 1 has 16/2 matches (8).
     * 4) Assert losers round 1 has 16/4 matches (4).
     *
     * Expected results:
     * - bracketSize == 16
     * - winners round 1 size == 8
     * - losers round 1 size == 4
     */
    @Test
    void buildsBracketWithNextPowerOfTwo() {
        Tournament tournament = Tournament.builder().name("Test").build();
        List<TournamentEntry> entries = buildEntries(10);

        TournamentBracketGraph graph = tournamentBracketBuilder.buildBracket(tournament, entries);

        assertEquals(16, tournament.getBracketSize());
        assertEquals(8, graph.getWinnersRounds().get(0).size());
        assertEquals(4, graph.getLosersRounds().get(0).size());
    }

    /**
     * Validates wiring of winners and losers advancement plus grand final wiring
     * using a basic 8-team bracket.
     *
     * Steps:
     * 1) Build an 8-team bracket.
     * 2) Assign deterministic IDs to all matches to mimic persisted entities.
     * 3) Wire winners advancement, losers advancement, winners/losers to grand final,
     *    and grand final reset routing.
     * 4) Inspect the first winners match wiring for winner and loser targets.
     * 5) Ensure the grand final has an ID (created in graph).
     *
     * Expected results:
     * - W1M1 winner advances to W2M1 slot 1.
     * - W1M1 loser drops to L1M1 slot 1.
     * - Grand final has a non-null ID.
     */
    @Test
    void wiresWinnerAndLoserAdvancement() {
        Tournament tournament = Tournament.builder().name("Test").build();
        List<TournamentEntry> entries = buildEntries(8);

        TournamentBracketGraph graph = tournamentBracketBuilder.buildBracket(tournament, entries);

        assignIds(graph.getAllMatches(), 10L);

        tournamentBracketBuilder.wireWinnersAdvancement(graph.getWinnersRounds());
        tournamentBracketBuilder.wireLosersAdvancement(graph.getWinnersRounds(), graph.getLosersRounds());
        tournamentBracketBuilder.wireLosersToGrandFinal(graph.getWinnersRounds(), graph.getLosersRounds(), graph.getGrandFinal());
        tournamentBracketBuilder.wireGrandFinalReset(graph.getGrandFinal(), graph.getGrandFinalReset());

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
     * Verifies series lengths and initial counters across all bracket types.
     *
     * Steps:
     * 1) Build a 4-team bracket.
     * 2) Assert every winners-bracket match has Bo5 series length and 0-0 counters.
     * 3) Assert every losers-bracket match has Bo5 series length.
     * 4) Assert grand final and grand final reset are Bo7 and correctly typed.
     *
     * Expected results:
     * - Winners/losers matches are Bo5 and counters are 0.
     * - Grand final and reset are Bo7 and have the correct bracket types.
     */
    @Test
    void assignsCorrectSeriesLengths() {
        Tournament tournament = Tournament.builder().name("SeriesTest").build();
        List<TournamentEntry> entries = buildEntries(4);

        TournamentBracketGraph graph = tournamentBracketBuilder.buildBracket(tournament, entries);

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
     * Verifies every match created by the builder starts with zeroed series wins.
     *
     * Steps:
     * 1) Build an 8-team bracket.
     * 2) Iterate all matches in the graph.
     * 3) Assert both team series win counters are 0 for every match.
     *
     * Expected results:
     * - teamOneSeriesWins == 0 for all matches.
     * - teamTwoSeriesWins == 0 for all matches.
     */
    @Test
    void seriesWinsInitializedToZero() {
        Tournament tournament = Tournament.builder().name("ZeroTest").build();
        List<TournamentEntry> entries = buildEntries(8);

        TournamentBracketGraph graph = tournamentBracketBuilder.buildBracket(tournament, entries);

        for (TournamentMatch match : graph.getAllMatches()) {
            assertEquals(0, match.getTeamOneSeriesWins(), "teamOneSeriesWins should start at 0");
            assertEquals(0, match.getTeamTwoSeriesWins(), "teamTwoSeriesWins should start at 0");
        }
    }

    /**
     * Verifies TournamentMatch.getWinsRequired for Bo5 and Bo7 series.
     *
     * Steps:
     * 1) Build a 4-team bracket.
     * 2) Read a winners-bracket match (Bo5) and assert winsRequired is 3.
     * 3) Read the grand final (Bo7) and assert winsRequired is 4.
     *
     * Expected results:
     * - Bo5 requires 3 wins.
     * - Bo7 requires 4 wins.
     */
    @Test
    void winsRequiredCalculation() {
        Tournament tournament = Tournament.builder().name("WinsReq").build();
        List<TournamentEntry> entries = buildEntries(4);

        TournamentBracketGraph graph = tournamentBracketBuilder.buildBracket(tournament, entries);

        // Bo5: (5+1)/2 = 3 wins required
        TournamentMatch winnersMatch = graph.getWinnersRounds().get(0).get(0);
        assertEquals(3, winnersMatch.getWinsRequired());

        // Bo7: (7+1)/2 = 4 wins required
        assertEquals(4, graph.getGrandFinal().getWinsRequired());
    }

    /**
     * Verifies seed ordering and bye placement when the entry list is not full.
     *
     * Steps:
     * 1) Build a bracket with seeds {3,1,2} for a size-4 bracket.
     * 2) Validate bracket size is 4.
     * 3) Assert round 1 has two matches.
     * 4) Assert the first match is seed 1 vs null (seed 4 bye).
     * 5) Assert the second match is seed 2 vs seed 3.
     *
     * Expected results:
     * - Seed order is [1,4,2,3] and missing seed 4 is null.
     * - Match 1: seed 1 vs null, Match 2: seed 2 vs seed 3.
     */
    @Test
    void buildBracketOrdersSeedsAndInsertsByes() {
        Tournament tournament = Tournament.builder().name("SeedOrder").build();
        List<TournamentEntry> entries = buildEntriesWithSeeds(3, 1, 2);

        TournamentBracketGraph graph = tournamentBracketBuilder.buildBracket(tournament, entries);

        assertEquals(4, tournament.getBracketSize());
        assertEquals(2, graph.getWinnersRounds().size());
        assertEquals(2, graph.getWinnersRounds().get(0).size());

        TournamentMatch match1 = graph.getWinnersRounds().get(0).get(0);
        TournamentMatch match2 = graph.getWinnersRounds().get(0).get(1);

        assertEquals(1, match1.getTeamOneEntry().getSeed());
        assertNull(match1.getTeamTwoEntry(), "Seed 4 should be a bye");
        assertEquals(2, match2.getTeamOneEntry().getSeed());
        assertEquals(3, match2.getTeamTwoEntry().getSeed());
    }

    /**
     * Verifies behavior for a single-team tournament.
     *
     * Steps:
     * 1) Build a bracket with one entry.
     * 2) Assert bracket size is 1.
     * 3) Assert winners rounds list contains one round with zero matches.
     * 4) Assert losers rounds are empty.
     * 5) Assert only grand final and reset matches exist.
     *
     * Expected results:
     * - bracketSize == 1
     * - winnersRounds size == 1 and winnersRounds[0] size == 0
     * - losersRounds is empty
     * - allMatches size == 2 (grand final + reset)
     */
    @Test
    void buildBracketHandlesSingleTeam() {
        Tournament tournament = Tournament.builder().name("Solo").build();
        List<TournamentEntry> entries = buildEntries(1);

        TournamentBracketGraph graph = tournamentBracketBuilder.buildBracket(tournament, entries);

        assertEquals(1, tournament.getBracketSize());
        assertEquals(1, graph.getWinnersRounds().size());
        assertEquals(0, graph.getWinnersRounds().get(0).size());
        assertTrue(graph.getLosersRounds().isEmpty());
        assertEquals(2, graph.getAllMatches().size());
        assertEquals(TournamentBracketType.GRAND_FINAL, graph.getGrandFinal().getBracketType());
        assertEquals(TournamentBracketType.GRAND_FINAL_RESET, graph.getGrandFinalReset().getBracketType());
    }

    /**
     * Verifies full winners/losers round structure and round numbering for 8 teams.
     *
     * Steps:
     * 1) Build an 8-team bracket.
     * 2) Assert winners rounds sizes [4,2,1] and losers rounds sizes [2,2,1,1,1].
     * 3) Assert allMatches count is 16 (14 bracket matches + grand final + reset).
     * 4) Assert every winners match is typed WINNERS and round numbers are 1..N.
     * 5) Assert every losers match is typed LOSERS and round numbers are 1..M.
     *
     * Expected results:
     * - Winners rounds sizes match the expected power-of-two progression.
     * - Losers rounds sizes match the double-elimination structure.
     * - All matches are correctly typed and numbered.
     */
    @Test
    void buildBracketCreatesExpectedRoundStructureForPowerOfTwo() {
        Tournament tournament = Tournament.builder().name("Structure").build();
        List<TournamentEntry> entries = buildEntries(8);

        TournamentBracketGraph graph = tournamentBracketBuilder.buildBracket(tournament, entries);

        assertEquals(8, tournament.getBracketSize());
        assertEquals(Arrays.asList(4, 2, 1), roundSizes(graph.getWinnersRounds()));
        assertEquals(Arrays.asList(2, 2, 1, 1, 1), roundSizes(graph.getLosersRounds()));
        assertEquals(16, graph.getAllMatches().size());

        for (int r = 0; r < graph.getWinnersRounds().size(); r++) {
            for (TournamentMatch match : graph.getWinnersRounds().get(r)) {
                assertEquals(TournamentBracketType.WINNERS, match.getBracketType());
                assertEquals(r + 1, match.getRoundNumber());
            }
        }

        for (int r = 0; r < graph.getLosersRounds().size(); r++) {
            for (TournamentMatch match : graph.getLosersRounds().get(r)) {
                assertEquals(TournamentBracketType.LOSERS, match.getBracketType());
                assertEquals(r + 1, match.getRoundNumber());
            }
        }
    }

    /**
     * Verifies winners-bracket advancement wiring and slot assignment.
     *
     * Steps:
     * 1) Build a 4-team bracket.
     * 2) Assign deterministic IDs to all matches.
     * 3) Wire winners advancement.
     * 4) Assert W1M1 points to W2M1 slot 1.
     * 5) Assert W1M2 points to W2M1 slot 2.
     * 6) Assert the final winners match has no next-winner target.
     *
     * Expected results:
     * - W1M1 next winner = W2M1 slot 1.
     * - W1M2 next winner = W2M1 slot 2.
     * - W2M1 has no next-winner wiring.
     */
    @Test
    void wireWinnersAdvancementSetsTargetsAndSlots() {
        Tournament tournament = Tournament.builder().name("WinnersWire").build();
        List<TournamentEntry> entries = buildEntries(4);

        TournamentBracketGraph graph = tournamentBracketBuilder.buildBracket(tournament, entries);
        assignIds(graph.getAllMatches(), 100L);

        tournamentBracketBuilder.wireWinnersAdvancement(graph.getWinnersRounds());

        TournamentMatch w1m1 = graph.getWinnersRounds().get(0).get(0);
        TournamentMatch w1m2 = graph.getWinnersRounds().get(0).get(1);
        TournamentMatch w2m1 = graph.getWinnersRounds().get(1).get(0);

        assertEquals(w2m1.getId(), w1m1.getNextWinnerMatchId());
        assertEquals(1, w1m1.getNextWinnerSlot());
        assertEquals(w2m1.getId(), w1m2.getNextWinnerMatchId());
        assertEquals(2, w1m2.getNextWinnerSlot());
        assertNull(w2m1.getNextWinnerMatchId());
    }

    /**
     * Verifies winners-bracket wiring is a no-op when only one round exists.
     *
     * Steps:
     * 1) Build a 2-team bracket (single winners round).
     * 2) Assign deterministic IDs.
     * 3) Wire winners advancement.
     * 4) Assert the only match has no next-winner target or slot.
     *
     * Expected results:
     * - nextWinnerMatchId and nextWinnerSlot remain null.
     */
    @Test
    void wireWinnersAdvancementNoNextRoundDoesNothing() {
        Tournament tournament = Tournament.builder().name("WinnersEdge").build();
        List<TournamentEntry> entries = buildEntries(2);

        TournamentBracketGraph graph = tournamentBracketBuilder.buildBracket(tournament, entries);
        assignIds(graph.getAllMatches(), 200L);

        tournamentBracketBuilder.wireWinnersAdvancement(graph.getWinnersRounds());

        TournamentMatch onlyMatch = graph.getWinnersRounds().get(0).get(0);
        assertNull(onlyMatch.getNextWinnerMatchId());
        assertNull(onlyMatch.getNextWinnerSlot());
    }

    /**
     * Verifies losers-bracket wiring returns early when losers rounds are empty.
     *
     * Steps:
     * 1) Build a 2-team bracket.
     * 2) Assign deterministic IDs.
     * 3) Call wireLosersAdvancement with an empty losers rounds list.
     * 4) Assert winners match has no loser routing set.
     *
     * Expected results:
     * - nextLoserMatchId and nextLoserSlot remain null.
     */
    @Test
    void wireLosersAdvancementEmptyLosersRoundsReturnsEarly() {
        Tournament tournament = Tournament.builder().name("LosersEmpty").build();
        List<TournamentEntry> entries = buildEntries(2);

        TournamentBracketGraph graph = tournamentBracketBuilder.buildBracket(tournament, entries);
        assignIds(graph.getAllMatches(), 300L);

        tournamentBracketBuilder.wireLosersAdvancement(graph.getWinnersRounds(), List.of());

        TournamentMatch w1m1 = graph.getWinnersRounds().get(0).get(0);
        assertNull(w1m1.getNextLoserMatchId());
        assertNull(w1m1.getNextLoserSlot());
    }

    /**
     * Verifies winners-to-losers drops and losers-bracket progression for 8 teams.
     *
     * Steps:
     * 1) Build an 8-team bracket and assign deterministic IDs.
     * 2) Wire losers advancement.
     * 3) Assert W1 losers drop into L1 with correct slots (1,2).
     * 4) Assert W2 losers drop into L2 with slot 2.
     * 5) Assert W3 loser drops into L4 with slot 2.
     * 6) Assert losers rounds advance:
     *    - Odd-indexed transitions keep 1:1 mapping into slot 1.
     *    - Even-indexed transitions merge into the next round with slots 1 and 2.
     *
     * Expected results:
     * - Specific nextLoserMatchId/nextLoserSlot mappings from winners rounds.
     * - Specific nextWinnerMatchId/nextWinnerSlot mappings between losers rounds.
     */
    @Test
    void wireLosersAdvancementMapsFromWinnersAndProgressesLosers() {
        Tournament tournament = Tournament.builder().name("LosersWire").build();
        List<TournamentEntry> entries = buildEntries(8);

        TournamentBracketGraph graph = tournamentBracketBuilder.buildBracket(tournament, entries);
        assignIds(graph.getAllMatches(), 400L);

        tournamentBracketBuilder.wireLosersAdvancement(graph.getWinnersRounds(), graph.getLosersRounds());

        TournamentMatch w1m1 = graph.getWinnersRounds().get(0).get(0);
        TournamentMatch w1m2 = graph.getWinnersRounds().get(0).get(1);
        TournamentMatch w1m3 = graph.getWinnersRounds().get(0).get(2);
        TournamentMatch w1m4 = graph.getWinnersRounds().get(0).get(3);
        TournamentMatch w2m1 = graph.getWinnersRounds().get(1).get(0);
        TournamentMatch w2m2 = graph.getWinnersRounds().get(1).get(1);
        TournamentMatch w3m1 = graph.getWinnersRounds().get(2).get(0);

        TournamentMatch l1m1 = graph.getLosersRounds().get(0).get(0);
        TournamentMatch l1m2 = graph.getLosersRounds().get(0).get(1);
        TournamentMatch l2m1 = graph.getLosersRounds().get(1).get(0);
        TournamentMatch l2m2 = graph.getLosersRounds().get(1).get(1);
        TournamentMatch l3m1 = graph.getLosersRounds().get(2).get(0);
        TournamentMatch l4m1 = graph.getLosersRounds().get(3).get(0);
        TournamentMatch l5m1 = graph.getLosersRounds().get(4).get(0);

        assertEquals(l1m1.getId(), w1m1.getNextLoserMatchId());
        assertEquals(1, w1m1.getNextLoserSlot());
        assertEquals(l1m1.getId(), w1m2.getNextLoserMatchId());
        assertEquals(2, w1m2.getNextLoserSlot());
        assertEquals(l1m2.getId(), w1m3.getNextLoserMatchId());
        assertEquals(1, w1m3.getNextLoserSlot());
        assertEquals(l1m2.getId(), w1m4.getNextLoserMatchId());
        assertEquals(2, w1m4.getNextLoserSlot());

        assertEquals(l2m1.getId(), w2m1.getNextLoserMatchId());
        assertEquals(2, w2m1.getNextLoserSlot());
        assertEquals(l2m2.getId(), w2m2.getNextLoserMatchId());
        assertEquals(2, w2m2.getNextLoserSlot());

        assertEquals(l4m1.getId(), w3m1.getNextLoserMatchId());
        assertEquals(2, w3m1.getNextLoserSlot());

        assertEquals(l2m1.getId(), l1m1.getNextWinnerMatchId());
        assertEquals(1, l1m1.getNextWinnerSlot());
        assertEquals(l2m2.getId(), l1m2.getNextWinnerMatchId());
        assertEquals(1, l1m2.getNextWinnerSlot());

        assertEquals(l3m1.getId(), l2m1.getNextWinnerMatchId());
        assertEquals(1, l2m1.getNextWinnerSlot());
        assertEquals(l3m1.getId(), l2m2.getNextWinnerMatchId());
        assertEquals(2, l2m2.getNextWinnerSlot());

        assertEquals(l4m1.getId(), l3m1.getNextWinnerMatchId());
        assertEquals(1, l3m1.getNextWinnerSlot());

        assertEquals(l5m1.getId(), l4m1.getNextWinnerMatchId());
        assertEquals(1, l4m1.getNextWinnerSlot());
    }

    /**
     * Verifies winners final is wired to grand final even when no losers rounds exist.
     *
     * Steps:
     * 1) Build a 2-team bracket (no losers rounds).
     * 2) Assign deterministic IDs.
     * 3) Wire losers-to-grand-final with an empty losers list.
     * 4) Assert winners final points to grand final slot 1.
     *
     * Expected results:
     * - Winners final has nextWinnerMatchId set to grand final and slot 1.
     */
    @Test
    void wireLosersToGrandFinalHandlesEmptyLosersRounds() {
        Tournament tournament = Tournament.builder().name("GrandFinalEdge").build();
        List<TournamentEntry> entries = buildEntries(2);

        TournamentBracketGraph graph = tournamentBracketBuilder.buildBracket(tournament, entries);
        assignIds(graph.getAllMatches(), 500L);

        tournamentBracketBuilder.wireLosersToGrandFinal(graph.getWinnersRounds(), List.of(), graph.getGrandFinal());

        TournamentMatch winnersFinal = graph.getWinnersRounds().get(0).get(0);
        assertEquals(graph.getGrandFinal().getId(), winnersFinal.getNextWinnerMatchId());
        assertEquals(1, winnersFinal.getNextWinnerSlot());
    }

    /**
     * Verifies both winners and losers bracket champions advance to grand final.
     *
     * Steps:
     * 1) Build a 4-team bracket.
     * 2) Assign deterministic IDs.
     * 3) Wire losers-to-grand-final.
     * 4) Assert winners final goes to grand final slot 1.
     * 5) Assert losers final goes to grand final slot 2.
     *
     * Expected results:
     * - Winners champion feeds slot 1, losers champion feeds slot 2.
     */
    @Test
    void wireLosersToGrandFinalSetsBothChampions() {
        Tournament tournament = Tournament.builder().name("GrandFinal").build();
        List<TournamentEntry> entries = buildEntries(4);

        TournamentBracketGraph graph = tournamentBracketBuilder.buildBracket(tournament, entries);
        assignIds(graph.getAllMatches(), 600L);

        tournamentBracketBuilder.wireLosersToGrandFinal(graph.getWinnersRounds(), graph.getLosersRounds(), graph.getGrandFinal());

        TournamentMatch winnersFinal = graph.getWinnersRounds().get(1).get(0);
        TournamentMatch losersFinal = graph.getLosersRounds().get(graph.getLosersRounds().size() - 1).get(0);

        assertEquals(graph.getGrandFinal().getId(), winnersFinal.getNextWinnerMatchId());
        assertEquals(1, winnersFinal.getNextWinnerSlot());
        assertEquals(graph.getGrandFinal().getId(), losersFinal.getNextWinnerMatchId());
        assertEquals(2, losersFinal.getNextWinnerSlot());
    }

    /**
     * Verifies grand final loser is routed to the reset match in multiple instances.
     *
     * Steps:
     * 1) Create two independent grand final/reset pairs.
     * 2) Assign IDs to each pair.
     * 3) Wire grand final reset for each pair.
     * 4) Assert each grand final points to its reset with slot 1.
     *
     * Expected results:
     * - Each grand final has nextLoserMatchId set to its reset and slot 1.
     */
    @Test
    void wireGrandFinalResetRoutesLoserToReset() {
        TournamentMatch grandFinalA = TournamentMatch.builder().build();
        TournamentMatch resetA = TournamentMatch.builder().build();
        setId(grandFinalA, 700L);
        setId(resetA, 701L);
        tournamentBracketBuilder.wireGrandFinalReset(grandFinalA, resetA);

        TournamentMatch grandFinalB = TournamentMatch.builder().build();
        TournamentMatch resetB = TournamentMatch.builder().build();
        setId(grandFinalB, 800L);
        setId(resetB, 801L);
        tournamentBracketBuilder.wireGrandFinalReset(grandFinalB, resetB);

        assertAll(
                () -> assertEquals(701L, grandFinalA.getNextLoserMatchId()),
                () -> assertEquals(1, grandFinalA.getNextLoserSlot()),
                () -> assertEquals(801L, grandFinalB.getNextLoserMatchId()),
                () -> assertEquals(1, grandFinalB.getNextLoserSlot())
        );
    }

    /**
     * Verifies private createMatch sets all base fields and series length by bracket type.
     *
     * Steps:
     * 1) Invoke createMatch via reflection for a WINNERS match with both teams.
     * 2) Invoke createMatch via reflection for a GRAND_FINAL match with null teams.
     * 3) Assert bracket type, round, index, and default state.
     * 4) Assert series length is Bo5 for winners, Bo7 for grand final.
     * 5) Assert series win counters start at 0 and teams are set correctly.
     *
     * Expected results:
     * - Winners match is Bo5, PENDING, 0-0, and has both teams.
     * - Grand final match is Bo7 and typed GRAND_FINAL.
     */
    @Test
    void createMatchAssignsSeriesLengthAndDefaults() {
        Tournament tournament = Tournament.builder().name("CreateMatch").build();
        TournamentEntry teamOne = buildEntry(1);
        TournamentEntry teamTwo = buildEntry(2);

        TournamentMatch winnersMatch = invokeCreateMatch(tournamentBracketBuilder, tournament, TournamentBracketType.WINNERS, 1, 1, teamOne, teamTwo);
        TournamentMatch grandFinalMatch = invokeCreateMatch(tournamentBracketBuilder, tournament, TournamentBracketType.GRAND_FINAL, 1, 1, null, null);

        assertAll(
                () -> assertEquals(TournamentBracketType.WINNERS, winnersMatch.getBracketType()),
                () -> assertEquals(1, winnersMatch.getRoundNumber()),
                () -> assertEquals(1, winnersMatch.getMatchIndex()),
                () -> assertEquals(TournamentMatchState.PENDING, winnersMatch.getState()),
                () -> assertEquals(TournamentBracketBuilder.NORMAL_SERIES_LENGTH, winnersMatch.getSeriesLength()),
                () -> assertEquals(0, winnersMatch.getTeamOneSeriesWins()),
                () -> assertEquals(0, winnersMatch.getTeamTwoSeriesWins()),
                () -> assertEquals(teamOne, winnersMatch.getTeamOneEntry()),
                () -> assertEquals(teamTwo, winnersMatch.getTeamTwoEntry()),
                () -> assertEquals(TournamentBracketBuilder.GRAND_FINAL_SERIES_LENGTH, grandFinalMatch.getSeriesLength()),
                () -> assertEquals(TournamentBracketType.GRAND_FINAL, grandFinalMatch.getBracketType())
        );
    }

    /**
     * Verifies nextPowerOfTwo handles edge values and typical inputs.
     *
     * Steps:
     * 1) Invoke nextPowerOfTwo via reflection for inputs 0,1,2,3,5.
     * 2) Assert each result is the smallest power of two >= input.
     *
     * Expected results:
     * - 0 -> 1
     * - 1 -> 1
     * - 2 -> 2
     * - 3 -> 4
     * - 5 -> 8
     */
    @Test
    void nextPowerOfTwoHandlesEdgeAndNormalValues() {
        assertAll(
                () -> assertEquals(1, invokeNextPowerOfTwo(0)),
                () -> assertEquals(1, invokeNextPowerOfTwo(1)),
                () -> assertEquals(2, invokeNextPowerOfTwo(2)),
                () -> assertEquals(4, invokeNextPowerOfTwo(3)),
                () -> assertEquals(8, invokeNextPowerOfTwo(5))
        );
    }

    /**
     * Verifies generateSeedOrder produces the standard high-low seeding pattern.
     *
     * Steps:
     * 1) Invoke generateSeedOrder via reflection for sizes 2, 4, and 8.
     * 2) Assert the generated sequence matches expected seed ordering.
     *
     * Expected results:
     * - Size 2: [1, 2]
     * - Size 4: [1, 4, 2, 3]
     * - Size 8: [1, 8, 4, 5, 2, 7, 3, 6]
     */
    @Test
    void generateSeedOrderReturnsExpectedOrder() {
        assertAll(
                () -> assertEquals(List.of(1, 2), invokeGenerateSeedOrder(2)),
                () -> assertEquals(List.of(1, 4, 2, 3), invokeGenerateSeedOrder(4)),
                () -> assertEquals(List.of(1, 8, 4, 5, 2, 7, 3, 6), invokeGenerateSeedOrder(8))
        );
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

    private static List<TournamentEntry> buildEntriesWithSeeds(int... seeds) {
        List<TournamentEntry> entries = new ArrayList<>();
        for (int seed : seeds) {
            entries.add(buildEntry(seed));
        }
        return entries;
    }

    private static TournamentEntry buildEntry(int seed) {
        Team team = new Team();
        team.setName("Team " + seed);
        team.setUuid(UUID.randomUUID());
        TournamentEntry entry = new TournamentEntry();
        entry.setTeam(team);
        entry.setSeed(seed);
        return entry;
    }

    private static List<Integer> roundSizes(List<List<TournamentMatch>> rounds) {
        List<Integer> sizes = new ArrayList<>();
        for (List<TournamentMatch> round : rounds) {
            sizes.add(round.size());
        }
        return sizes;
    }

    private static void assignIds(List<TournamentMatch> matches, long startId) {
        long current = startId;
        for (TournamentMatch match : matches) {
            setId(match, current++);
        }
    }

    private static void setId(BaseEntity entity, long id) {
        try {
            Field field = BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int invokeNextPowerOfTwo(int n) {
        try {
            Method method = TournamentBracketBuilder.class.getDeclaredMethod("nextPowerOfTwo", int.class);
            method.setAccessible(true);
            return (int) method.invoke(null, n);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> invokeGenerateSeedOrder(int size) {
        try {
            Method method = TournamentBracketBuilder.class.getDeclaredMethod("generateSeedOrder", int.class);
            method.setAccessible(true);
            return (List<Integer>) method.invoke(null, size);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TournamentMatch invokeCreateMatch(TournamentBracketBuilder builder,
                                                     Tournament tournament,
                                                     TournamentBracketType bracketType,
                                                     int round,
                                                     int matchIndex,
                                                     TournamentEntry teamOne,
                                                     TournamentEntry teamTwo) {
        try {
            Method method = TournamentBracketBuilder.class.getDeclaredMethod(
                    "createMatch",
                    Tournament.class,
                    TournamentBracketType.class,
                    int.class,
                    int.class,
                    TournamentEntry.class,
                    TournamentEntry.class
            );
            method.setAccessible(true);
            return (TournamentMatch) method.invoke(builder, tournament, bracketType, round, matchIndex, teamOne, teamTwo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

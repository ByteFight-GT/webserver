package org.bytefight.webserver.tournament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.ladder.domain.DefaultLadderSettings;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.ladder.infra.LadderRepository;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.bytefight.webserver.tournament.application.TournamentBracketBuilder;
import org.bytefight.webserver.tournament.application.TournamentBracketGraph;
import org.bytefight.webserver.tournament.application.TournamentMatchScheduler;
import org.bytefight.webserver.tournament.application.TournamentResultHandler;
import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.tournament.domain.TournamentBracketType;
import org.bytefight.webserver.tournament.domain.TournamentEntry;
import org.bytefight.webserver.tournament.domain.TournamentEntryStatus;
import org.bytefight.webserver.tournament.domain.TournamentGame;
import org.bytefight.webserver.tournament.domain.TournamentMatch;
import org.bytefight.webserver.tournament.domain.TournamentMatchState;
import org.bytefight.webserver.tournament.domain.TournamentStatus;
import org.bytefight.webserver.tournament.infra.TournamentEntryRepository;
import org.bytefight.webserver.tournament.infra.TournamentGameRepository;
import org.bytefight.webserver.tournament.infra.TournamentMatchRepository;
import org.bytefight.webserver.tournament.infra.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the best-of series result handler.
 *
 * <p>These tests verify: - Series win tracking (individual game wins increment series counters) -
 * Series completion (match completes when one side reaches the win threshold) - Series continuation
 * (next game queued when series isn't decided) - Draw handling (draws queue a new game without
 * changing scores) - Bracket-level loss tracking and elimination (series loss = 1 bracket loss)
 */
public class TournamentResultHandlerIntegrationTest extends FullStackIntegrationTestBase {

  @Autowired private TournamentResultHandler tournamentResultHandler;

  @Autowired private TournamentRepository tournamentRepository;

  @Autowired private TournamentEntryRepository tournamentEntryRepository;

  @Autowired private TournamentMatchRepository tournamentMatchRepository;

  @Autowired private TournamentGameRepository tournamentGameRepository;

  @Autowired private CompetitionRepository competitionRepository;

  @Autowired private TeamRepository teamRepository;

  @Autowired private FileRecordRepository fileRecordRepository;

  @Autowired private SubmissionRepository submissionRepository;

  @Autowired private LadderRepository ladderRepository;

  @Autowired private GameMatchService gameMatchService;

  @Autowired private TournamentBracketBuilder tournamentBracketBuilder;

  @Autowired private TournamentMatchScheduler tournamentMatchScheduler;

  // ── Bo5 series tests ────────────────────────────────────────────────────

  /**
   * Simulates a single game win in a Bo5 series. After 1 win, the series should still be active
   * (QUEUED) since 3 wins are needed.
   */
  @Test
  void singleGameWinDoesNotCompleteBo5Series() {
    Competition competition = createCompetition("comp-single-win", true);
    Tournament tournament = createTournament(competition);

    Team teamA = createTeamWithSubmission(competition, "Alpha");
    Team teamB = createTeamWithSubmission(competition, "Beta");

    TournamentEntry entryA = createEntry(tournament, teamA, 1);
    TournamentEntry entryB = createEntry(tournament, teamB, 2);

    // Create a Bo5 series match and link game 1.
    TournamentMatch match =
        createMatch(
            tournament,
            entryA,
            entryB,
            TournamentBracketType.WINNERS,
            TournamentBracketBuilder.NORMAL_SERIES_LENGTH);
    GameMatch gameMatch1 = createGameMatch(teamA, teamB);
    createTournamentGame(match, gameMatch1, 1);
    match.setState(TournamentMatchState.QUEUED);
    tournamentMatchRepository.save(match);

    // Team A wins game 1.
    tournamentResultHandler.handleTournamentResult(gameMatch1, MatchStatus.team_a_win);

    TournamentMatch refreshed = tournamentMatchRepository.findById(match.getId()).orElseThrow();
    assertEquals(1, refreshed.getTeamOneSeriesWins(), "Team A should have 1 series win");
    assertEquals(0, refreshed.getTeamTwoSeriesWins(), "Team B should have 0 series wins");
    // Series is not decided yet (need 3 for Bo5), so match stays QUEUED.
    assertEquals(
        TournamentMatchState.QUEUED, refreshed.getState(), "Series should still be active");
    assertNull(refreshed.getWinnerEntry(), "No winner yet");
    assertNull(refreshed.getLoserEntry(), "No loser yet");

    // A new game should have been queued (game 2).
    List<TournamentGame> games =
        tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(refreshed);
    assertEquals(2, games.size(), "Game 2 should have been queued");
    assertEquals(2, games.get(1).getGameNumber());
  }

  /**
   * Simulates a full Bo5 series where team A wins 3-0 (sweep). After the 3rd win, the series should
   * complete and the loser gets 1 bracket loss.
   */
  @Test
  void bo5SweepCompletesSeriesAndIncrementsLoss() {
    Competition competition = createCompetition("comp-sweep", true);
    Tournament tournament = createTournament(competition);

    Team teamA = createTeamWithSubmission(competition, "Alpha");
    Team teamB = createTeamWithSubmission(competition, "Beta");

    TournamentEntry entryA = createEntry(tournament, teamA, 1);
    TournamentEntry entryB = createEntry(tournament, teamB, 2);

    TournamentMatch match =
        createMatch(
            tournament,
            entryA,
            entryB,
            TournamentBracketType.WINNERS,
            TournamentBracketBuilder.NORMAL_SERIES_LENGTH);
    match.setState(TournamentMatchState.QUEUED);

    // Game 1: team A wins
    GameMatch gm1 = createGameMatch(teamA, teamB);
    createTournamentGame(match, gm1, 1);
    tournamentMatchRepository.save(match);
    tournamentResultHandler.handleTournamentResult(gm1, MatchStatus.team_a_win);

    // Game 2: team A wins
    TournamentMatch afterG1 = tournamentMatchRepository.findById(match.getId()).orElseThrow();
    List<TournamentGame> gamesAfterG1 =
        tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(afterG1);
    GameMatch gm2 = gamesAfterG1.get(1).getGameMatch(); // game 2 was auto-queued
    tournamentResultHandler.handleTournamentResult(gm2, MatchStatus.team_a_win);

    // Game 3: team A wins (series decided!)
    TournamentMatch afterG2 = tournamentMatchRepository.findById(match.getId()).orElseThrow();
    List<TournamentGame> gamesAfterG2 =
        tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(afterG2);
    GameMatch gm3 = gamesAfterG2.get(2).getGameMatch(); // game 3 was auto-queued
    tournamentResultHandler.handleTournamentResult(gm3, MatchStatus.team_a_win);

    // Verify series completed.
    TournamentMatch completed = tournamentMatchRepository.findById(match.getId()).orElseThrow();
    assertEquals(3, completed.getTeamOneSeriesWins());
    assertEquals(0, completed.getTeamTwoSeriesWins());
    assertEquals(TournamentMatchState.COMPLETE, completed.getState());
    assertEquals(entryA.getId(), completed.getWinnerEntry().getId());
    assertEquals(entryB.getId(), completed.getLoserEntry().getId());

    // Loser gets 1 bracket loss.
    TournamentEntry loser = tournamentEntryRepository.findById(entryB.getId()).orElseThrow();
    assertEquals(1, loser.getLosses());
    assertEquals(
        TournamentEntryStatus.ACTIVE,
        loser.getStatus(),
        "First bracket loss should not eliminate (double elimination)");
  }

  /** Simulates two series losses to verify elimination in double elimination. */
  @Test
  void twoSeriesLossesEliminateEntry() {
    Competition competition = createCompetition("comp-elim", true);
    Tournament tournament = createTournament(competition);

    Team teamA = createTeamWithSubmission(competition, "Alpha");
    Team teamB = createTeamWithSubmission(competition, "Beta");

    TournamentEntry entryA = createEntry(tournament, teamA, 1);
    TournamentEntry entryB = createEntry(tournament, teamB, 2);

    // ── First series: team A sweeps 3-0 ──
    TournamentMatch match1 =
        createMatch(
            tournament,
            entryA,
            entryB,
            TournamentBracketType.WINNERS,
            TournamentBracketBuilder.NORMAL_SERIES_LENGTH);
    match1.setState(TournamentMatchState.QUEUED);
    simulateSeriesWin(match1, teamA, teamB, MatchStatus.team_a_win, 3);

    TournamentEntry afterFirst = tournamentEntryRepository.findById(entryB.getId()).orElseThrow();
    assertEquals(1, afterFirst.getLosses());
    assertEquals(TournamentEntryStatus.ACTIVE, afterFirst.getStatus());

    // ── Second series: team A sweeps again 3-0 ──
    TournamentMatch match2 =
        createMatch(
            tournament,
            entryA,
            entryB,
            TournamentBracketType.LOSERS,
            TournamentBracketBuilder.NORMAL_SERIES_LENGTH);
    match2.setState(TournamentMatchState.QUEUED);
    simulateSeriesWin(match2, teamA, teamB, MatchStatus.team_a_win, 3);

    TournamentEntry afterSecond = tournamentEntryRepository.findById(entryB.getId()).orElseThrow();
    assertEquals(2, afterSecond.getLosses());
    assertEquals(TournamentEntryStatus.ELIMINATED, afterSecond.getStatus());
    assertNotNull(afterSecond.getEliminatedAt());
  }

  // ── Draw handling ───────────────────────────────────────────────────────

  /** Draws in a series should queue a new game without changing series scores. */
  @Test
  void drawQueuesNewGameWithoutChangingSeriesScore() {
    Competition competition = createCompetition("comp-draw", true);
    Tournament tournament = createTournament(competition);

    Team teamA = createTeamWithSubmission(competition, "Alpha");
    Team teamB = createTeamWithSubmission(competition, "Beta");

    TournamentEntry entryA = createEntry(tournament, teamA, 1);
    TournamentEntry entryB = createEntry(tournament, teamB, 2);

    TournamentMatch match =
        createMatch(
            tournament,
            entryA,
            entryB,
            TournamentBracketType.WINNERS,
            TournamentBracketBuilder.NORMAL_SERIES_LENGTH);
    GameMatch gm1 = createGameMatch(teamA, teamB);
    createTournamentGame(match, gm1, 1);
    match.setState(TournamentMatchState.QUEUED);
    tournamentMatchRepository.save(match);

    // Game 1 is a draw.
    tournamentResultHandler.handleTournamentResult(gm1, MatchStatus.draw);

    TournamentMatch refreshed = tournamentMatchRepository.findById(match.getId()).orElseThrow();
    assertEquals(0, refreshed.getTeamOneSeriesWins(), "Draw should not change team A wins");
    assertEquals(0, refreshed.getTeamTwoSeriesWins(), "Draw should not change team B wins");
    assertEquals(TournamentMatchState.QUEUED, refreshed.getState(), "Match should still be active");

    // A new game should have been queued (game 2).
    List<TournamentGame> games =
        tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(refreshed);
    assertEquals(2, games.size(), "A new game should be queued after draw");
    assertNotNull(games.get(1).getGameMatch(), "Game 2 should have a GameMatch");
  }

  /** If 10 games are reached with equal series wins, the higher-seeded entry wins. */
  @Test
  void tenGameCapUsesSeedTiebreakWhenSeriesWinsAreEqual() {
    Competition competition = createCompetition("comp-multi-draw", true);
    Tournament tournament = createTournament(competition);

    Team teamA = createTeamWithSubmission(competition, "Alpha");
    Team teamB = createTeamWithSubmission(competition, "Beta");

    // Team B has the higher seed (1 is strongest seed).
    TournamentEntry entryA = createEntry(tournament, teamA, 2);
    TournamentEntry entryB = createEntry(tournament, teamB, 1);

    TournamentMatch match =
        createMatch(
            tournament,
            entryA,
            entryB,
            TournamentBracketType.WINNERS,
            TournamentBracketBuilder.NORMAL_SERIES_LENGTH);
    GameMatch gm1 = createGameMatch(teamA, teamB);
    createTournamentGame(match, gm1, 1);
    match.setState(TournamentMatchState.QUEUED);
    tournamentMatchRepository.save(match);

    GameMatch currentGame = gm1;
    for (int gameNumber = 1; gameNumber <= 10; gameNumber++) {
      tournamentResultHandler.handleTournamentResult(currentGame, MatchStatus.draw);
      TournamentMatch refreshed = tournamentMatchRepository.findById(match.getId()).orElseThrow();
      if (gameNumber < 10) {
        assertEquals(TournamentMatchState.QUEUED, refreshed.getState());
        currentGame = getLatestAutoQueuedGame(refreshed);
      }
    }

    TournamentMatch completed = tournamentMatchRepository.findById(match.getId()).orElseThrow();
    assertEquals(0, completed.getTeamOneSeriesWins());
    assertEquals(0, completed.getTeamTwoSeriesWins());
    assertEquals(TournamentMatchState.COMPLETE, completed.getState());
    assertEquals(
        entryB.getId(),
        completed.getWinnerEntry().getId(),
        "Higher seed should win ties when the 10-game cap is reached");
    assertEquals(entryA.getId(), completed.getLoserEntry().getId());

    List<TournamentGame> allGames =
        tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(completed);
    assertEquals(10, allGames.size(), "Series should stop at 10 total games");
  }

  /** If 10 games are reached and wins are unequal, more wins beats seed. */
  @Test
  void tenGameCapUsesSeriesWinsBeforeSeed() {
    Competition competition = createCompetition("comp-cap-wins-first", true);
    Tournament tournament = createTournament(competition);

    Team teamA = createTeamWithSubmission(competition, "Alpha");
    Team teamB = createTeamWithSubmission(competition, "Beta");

    // Team B has the higher seed, but Team A will have more wins at game 10.
    TournamentEntry entryA = createEntry(tournament, teamA, 2);
    TournamentEntry entryB = createEntry(tournament, teamB, 1);

    TournamentMatch match =
        createMatch(
            tournament,
            entryA,
            entryB,
            TournamentBracketType.WINNERS,
            TournamentBracketBuilder.NORMAL_SERIES_LENGTH);
    GameMatch gm1 = createGameMatch(teamA, teamB);
    createTournamentGame(match, gm1, 1);
    match.setState(TournamentMatchState.QUEUED);
    tournamentMatchRepository.save(match);

    MatchStatus[] results =
        new MatchStatus[] {
          MatchStatus.team_a_win,
          MatchStatus.draw,
          MatchStatus.team_b_win,
          MatchStatus.draw,
          MatchStatus.team_a_win,
          MatchStatus.draw,
          MatchStatus.draw,
          MatchStatus.draw,
          MatchStatus.draw,
          MatchStatus.draw
        };

    GameMatch currentGame = gm1;
    for (int i = 0; i < results.length; i++) {
      tournamentResultHandler.handleTournamentResult(currentGame, results[i]);
      TournamentMatch refreshed = tournamentMatchRepository.findById(match.getId()).orElseThrow();
      if (i < results.length - 1) {
        assertEquals(TournamentMatchState.QUEUED, refreshed.getState());
        currentGame = getLatestAutoQueuedGame(refreshed);
      }
    }

    TournamentMatch completed = tournamentMatchRepository.findById(match.getId()).orElseThrow();
    assertEquals(2, completed.getTeamOneSeriesWins());
    assertEquals(1, completed.getTeamTwoSeriesWins());
    assertEquals(TournamentMatchState.COMPLETE, completed.getState());
    assertEquals(
        entryA.getId(),
        completed.getWinnerEntry().getId(),
        "More series wins should win before seed at the 10-game cap");
    assertEquals(entryB.getId(), completed.getLoserEntry().getId());

    List<TournamentGame> allGames =
        tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(completed);
    assertEquals(10, allGames.size(), "Series should stop at 10 total games");
  }

  // ── Bo7 grand final tests ───────────────────────────────────────────────

  /**
   * Grand final matches should require 4 wins (Bo7). Verifies that 3 wins do NOT complete the
   * series in a Bo7.
   */
  @Test
  void bo7GrandFinalRequiresFourWins() {
    Competition competition = createCompetition("comp-bo7", true);
    Tournament tournament = createTournament(competition);

    Team teamA = createTeamWithSubmission(competition, "Alpha");
    Team teamB = createTeamWithSubmission(competition, "Beta");

    TournamentEntry entryA = createEntry(tournament, teamA, 1);
    TournamentEntry entryB = createEntry(tournament, teamB, 2);

    TournamentMatch grandFinal =
        createMatch(
            tournament,
            entryA,
            entryB,
            TournamentBracketType.GRAND_FINAL,
            TournamentBracketBuilder.GRAND_FINAL_SERIES_LENGTH);
    grandFinal.setState(TournamentMatchState.QUEUED);

    // Team A wins 3 games — NOT enough for Bo7.
    GameMatch gm1 = createGameMatch(teamA, teamB);
    createTournamentGame(grandFinal, gm1, 1);
    tournamentMatchRepository.save(grandFinal);
    tournamentResultHandler.handleTournamentResult(gm1, MatchStatus.team_a_win);

    List<TournamentGame> g2 =
        tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(grandFinal);
    tournamentResultHandler.handleTournamentResult(
        g2.get(1).getGameMatch(), MatchStatus.team_a_win);

    List<TournamentGame> g3 =
        tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(grandFinal);
    tournamentResultHandler.handleTournamentResult(
        g3.get(2).getGameMatch(), MatchStatus.team_a_win);

    // After 3 wins, Bo7 should NOT be decided yet.
    TournamentMatch afterThree =
        tournamentMatchRepository.findById(grandFinal.getId()).orElseThrow();
    assertEquals(3, afterThree.getTeamOneSeriesWins());
    assertEquals(
        TournamentMatchState.QUEUED,
        afterThree.getState(),
        "Bo7 should not be decided after only 3 wins");

    // Game 4: team A wins (4th win, series decided).
    List<TournamentGame> g4 =
        tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(grandFinal);
    tournamentResultHandler.handleTournamentResult(
        g4.get(3).getGameMatch(), MatchStatus.team_a_win);

    TournamentMatch decided = tournamentMatchRepository.findById(grandFinal.getId()).orElseThrow();
    assertEquals(4, decided.getTeamOneSeriesWins());
    assertEquals(TournamentMatchState.COMPLETE, decided.getState());
  }

  /** Simulates a full Bo5 series that goes to game 5 (3-2) with alternating wins. */
  @Test
  void bo5GoesToFiveGamesWithAlternatingWins() {
    Competition competition = createCompetition("comp-g5", true);
    Tournament tournament = createTournament(competition);

    Team teamA = createTeamWithSubmission(competition, "Alpha");
    Team teamB = createTeamWithSubmission(competition, "Beta");

    TournamentEntry entryA = createEntry(tournament, teamA, 1);
    TournamentEntry entryB = createEntry(tournament, teamB, 2);

    TournamentMatch match =
        createMatch(
            tournament,
            entryA,
            entryB,
            TournamentBracketType.WINNERS,
            TournamentBracketBuilder.NORMAL_SERIES_LENGTH);
    match.setState(TournamentMatchState.QUEUED);

    // Game 1: A wins (1-0)
    GameMatch gm1 = createGameMatch(teamA, teamB);
    createTournamentGame(match, gm1, 1);
    tournamentMatchRepository.save(match);
    tournamentResultHandler.handleTournamentResult(gm1, MatchStatus.team_a_win);

    // Game 2: B wins (1-1)
    GameMatch gm2 = getLatestAutoQueuedGame(match);
    tournamentResultHandler.handleTournamentResult(gm2, MatchStatus.team_b_win);

    // Game 3: A wins (2-1)
    GameMatch gm3 = getLatestAutoQueuedGame(match);
    tournamentResultHandler.handleTournamentResult(gm3, MatchStatus.team_a_win);

    // Game 4: B wins (2-2)
    GameMatch gm4 = getLatestAutoQueuedGame(match);
    tournamentResultHandler.handleTournamentResult(gm4, MatchStatus.team_b_win);

    // After game 4, still not decided.
    TournamentMatch afterFour = tournamentMatchRepository.findById(match.getId()).orElseThrow();
    assertEquals(2, afterFour.getTeamOneSeriesWins());
    assertEquals(2, afterFour.getTeamTwoSeriesWins());
    assertEquals(TournamentMatchState.QUEUED, afterFour.getState());

    // Game 5: A wins (3-2, series decided!)
    GameMatch gm5 = getLatestAutoQueuedGame(match);
    tournamentResultHandler.handleTournamentResult(gm5, MatchStatus.team_a_win);

    TournamentMatch decided = tournamentMatchRepository.findById(match.getId()).orElseThrow();
    assertEquals(3, decided.getTeamOneSeriesWins());
    assertEquals(2, decided.getTeamTwoSeriesWins());
    assertEquals(TournamentMatchState.COMPLETE, decided.getState());
    assertEquals(entryA.getId(), decided.getWinnerEntry().getId());
    assertEquals(entryB.getId(), decided.getLoserEntry().getId());

    // Total games played: 5 (1 manual + 4 auto-queued)
    List<TournamentGame> allGames =
        tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(decided);
    assertEquals(5, allGames.size());
  }

  @Test
  void sixTeamBracketResultsAdvanceCorrectlyFromRoundOne() {
    Competition competition = createCompetition("comp-six-results", true);
    Tournament tournament = createTournament(competition);

    Team team1 = createTeamWithSubmission(competition, "Seed 1");
    Team team2 = createTeamWithSubmission(competition, "Seed 2");
    Team team3 = createTeamWithSubmission(competition, "Seed 3");
    Team team4 = createTeamWithSubmission(competition, "Seed 4");
    Team team5 = createTeamWithSubmission(competition, "Seed 5");
    Team team6 = createTeamWithSubmission(competition, "Seed 6");

    TournamentEntry seed1 = createEntry(tournament, team1, 1);
    TournamentEntry seed2 = createEntry(tournament, team2, 2);
    TournamentEntry seed3 = createEntry(tournament, team3, 3);
    TournamentEntry seed4 = createEntry(tournament, team4, 4);
    TournamentEntry seed5 = createEntry(tournament, team5, 5);
    TournamentEntry seed6 = createEntry(tournament, team6, 6);

    TournamentBracketGraph graph =
        tournamentBracketBuilder.buildBracket(
            tournament, List.of(seed1, seed2, seed3, seed4, seed5, seed6));
    tournamentMatchRepository.saveAll(graph.getAllMatches());
    tournamentBracketBuilder.wireWinnersAdvancement(graph.getWinnersRounds());
    tournamentBracketBuilder.wireLosersAdvancement(
        graph.getWinnersRounds(), graph.getLosersRounds());
    tournamentBracketBuilder.wireLosersToGrandFinal(
        graph.getWinnersRounds(), graph.getLosersRounds(), graph.getGrandFinal());
    tournamentBracketBuilder.wireGrandFinalReset(graph.getGrandFinal(), graph.getGrandFinalReset());
    tournamentMatchRepository.saveAll(graph.getAllMatches());

    tournamentMatchScheduler.processTournament(tournament);

    TournamentMatch w1m2 = findMatch(tournament, TournamentBracketType.WINNERS, 1, 2);
    TournamentMatch w1m4 = findMatch(tournament, TournamentBracketType.WINNERS, 1, 4);
    assertEquals(TournamentMatchState.QUEUED, w1m2.getState());
    assertEquals(TournamentMatchState.QUEUED, w1m4.getState());
    assertEquals(4, seedOf(w1m2.getTeamOneEntry()));
    assertEquals(5, seedOf(w1m2.getTeamTwoEntry()));
    assertEquals(3, seedOf(w1m4.getTeamOneEntry()));
    assertEquals(6, seedOf(w1m4.getTeamTwoEntry()));

    completeSeriesWithStraightWins(w1m2, MatchStatus.team_a_win);
    completeSeriesWithStraightWins(w1m4, MatchStatus.team_a_win);

    TournamentMatch refreshedW2m1 = findMatch(tournament, TournamentBracketType.WINNERS, 2, 1);
    TournamentMatch refreshedW2m2 = findMatch(tournament, TournamentBracketType.WINNERS, 2, 2);
    TournamentMatch refreshedL1m1 = findMatch(tournament, TournamentBracketType.LOSERS, 1, 1);
    TournamentMatch refreshedL1m2 = findMatch(tournament, TournamentBracketType.LOSERS, 1, 2);
    TournamentMatch refreshedL2m1 = findMatch(tournament, TournamentBracketType.LOSERS, 2, 1);
    TournamentMatch refreshedL2m2 = findMatch(tournament, TournamentBracketType.LOSERS, 2, 2);

    assertEquals(TournamentMatchState.QUEUED, refreshedW2m1.getState());
    assertEquals(1, seedOf(refreshedW2m1.getTeamOneEntry()));
    assertEquals(4, seedOf(refreshedW2m1.getTeamTwoEntry()));

    assertEquals(TournamentMatchState.QUEUED, refreshedW2m2.getState());
    assertEquals(2, seedOf(refreshedW2m2.getTeamOneEntry()));
    assertEquals(3, seedOf(refreshedW2m2.getTeamTwoEntry()));

    assertEquals(TournamentMatchState.SKIPPED, refreshedL1m1.getState());
    assertEquals(5, seedOf(refreshedL1m1.getWinnerEntry()));
    assertNull(refreshedL1m1.getLoserEntry());

    assertEquals(TournamentMatchState.SKIPPED, refreshedL1m2.getState());
    assertEquals(6, seedOf(refreshedL1m2.getWinnerEntry()));
    assertNull(refreshedL1m2.getLoserEntry());

    assertEquals(TournamentMatchState.PENDING, refreshedL2m1.getState());
    assertEquals(5, seedOf(refreshedL2m1.getTeamOneEntry()));
    assertNull(refreshedL2m1.getTeamTwoEntry());

    assertEquals(TournamentMatchState.PENDING, refreshedL2m2.getState());
    assertEquals(6, seedOf(refreshedL2m2.getTeamOneEntry()));
    assertNull(refreshedL2m2.getTeamTwoEntry());
  }

  // ── Helper methods ──────────────────────────────────────────────────────

  private Competition createCompetition(String slug, boolean active) {
    Competition competition = new Competition();
    competition.setSlug(slug + "-" + UUID.randomUUID());
    competition.setName("Competition " + slug);
    competition.setActive(active);
    competition.setWhitelisted(false);
    competition.setMaxPlayersPerTeam(2);
    Competition saved = competitionRepository.save(competition);
    ensureTournamentLadder(saved);
    return saved;
  }

  private void ensureTournamentLadder(Competition competition) {
    if (ladderRepository.findByCompetitionAndLadder(competition, "tournament").isPresent()) {
      return;
    }
    Ladder ladder = DefaultLadderSettings.baseline1500NoInflation();
    ladder.setCompetition(competition);
    ladder.setLadder("tournament");
    ladderRepository.save(ladder);
  }

  private Tournament createTournament(Competition competition) {
    Tournament tournament =
        Tournament.builder()
            .competition(competition)
            .name("Tournament")
            .status(TournamentStatus.IN_PROGRESS)
            .build();
    return tournamentRepository.save(tournament);
  }

  private Team createTeamWithSubmission(Competition competition, String name) {
    Team team = new Team();
    team.setCompetition(competition);
    team.setUuid(UUID.randomUUID());
    team.setName(name);
    team.setDisplayMembers(false);
    team.setJoinCode("JOIN-" + UUID.randomUUID().toString().substring(0, 6));
    teamRepository.save(team);

    FileRecord fileRecord =
        FileRecord.builder()
            .uuid(UUID.randomUUID())
            .filename("bot.jar")
            .contentType("application/java-archive")
            .size(1L)
            .sha256(UUID.randomUUID().toString())
            .storagePath("/tmp/" + UUID.randomUUID())
            .build();
    fileRecordRepository.save(fileRecord);

    Submission submission = new Submission();
    submission.setUuid(UUID.randomUUID());
    submission.setFileRecord(fileRecord);
    submission.setTeam(team);
    submissionRepository.save(submission);

    team.setCurrentSubmission(submission);
    teamRepository.save(team);
    return team;
  }

  private TournamentEntry createEntry(Tournament tournament, Team team, int seed) {
    TournamentEntry entry =
        TournamentEntry.builder().tournament(tournament).team(team).seed(seed).build();
    return tournamentEntryRepository.save(entry);
  }

  /** Creates a TournamentMatch (series node) with the given bracket type and series length. */
  private TournamentMatch createMatch(
      Tournament tournament,
      TournamentEntry teamOne,
      TournamentEntry teamTwo,
      TournamentBracketType bracketType,
      int seriesLength) {
    TournamentMatch match =
        TournamentMatch.builder()
            .tournament(tournament)
            .bracketType(bracketType)
            .roundNumber(1)
            .matchIndex(1)
            .teamOneEntry(teamOne)
            .teamTwoEntry(teamTwo)
            .state(TournamentMatchState.PENDING)
            .seriesLength(seriesLength)
            .teamOneSeriesWins(0)
            .teamTwoSeriesWins(0)
            .build();
    return tournamentMatchRepository.save(match);
  }

  /** Creates a GameMatch between two teams (for use in a series). */
  private GameMatch createGameMatch(Team teamA, Team teamB) {
    return gameMatchService.createMatch(
        null,
        teamA,
        teamB,
        teamA.getCurrentSubmission(),
        teamB.getCurrentSubmission(),
        "tournament",
        MatchReason.tournament,
        null,
        null);
  }

  /** Creates a TournamentGame linking a GameMatch to a TournamentMatch series. */
  private TournamentGame createTournamentGame(
      TournamentMatch match, GameMatch gameMatch, int gameNumber) {
    TournamentGame game =
        TournamentGame.builder()
            .tournamentMatch(match)
            .gameMatch(gameMatch)
            .gameNumber(gameNumber)
            .build();
    return tournamentGameRepository.save(game);
  }

  /**
   * Gets the GameMatch from the most recently auto-queued TournamentGame. This is used to chain
   * results in a series test.
   */
  private GameMatch getLatestAutoQueuedGame(TournamentMatch match) {
    List<TournamentGame> games =
        tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(match);
    return games.get(games.size() - 1).getGameMatch();
  }

  private TournamentMatch findMatch(
      Tournament tournament, TournamentBracketType type, int round, int index) {
    return tournamentMatchRepository
        .findByTournamentOrderByBracketTypeAscRoundNumberAscMatchIndexAsc(tournament)
        .stream()
        .filter(match -> match.getBracketType() == type)
        .filter(match -> match.getRoundNumber() == round)
        .filter(match -> match.getMatchIndex() == index)
        .findFirst()
        .orElseThrow();
  }

  private int seedOf(TournamentEntry entry) {
    return tournamentEntryRepository.findById(entry.getId()).orElseThrow().getSeed();
  }

  private void completeSeriesWithStraightWins(TournamentMatch match, MatchStatus winnerStatus) {
    int winsRequired =
        tournamentMatchRepository.findById(match.getId()).orElseThrow().getWinsRequired();
    for (int i = 0; i < winsRequired; i++) {
      TournamentMatch refreshed = tournamentMatchRepository.findById(match.getId()).orElseThrow();
      GameMatch game = getLatestAutoQueuedGame(refreshed);
      tournamentResultHandler.handleTournamentResult(game, winnerStatus);
    }
  }

  /**
   * Simulates N consecutive wins for one side of a series. Creates game 1 manually, then chains
   * auto-queued games for subsequent wins.
   */
  private void simulateSeriesWin(
      TournamentMatch match,
      Team winnerTeam,
      Team loserTeam,
      MatchStatus winStatus,
      int gamesNeeded) {
    // Game 1 (manual setup).
    GameMatch gm1 = createGameMatch(winnerTeam, loserTeam);
    createTournamentGame(match, gm1, 1);
    tournamentMatchRepository.save(match);
    tournamentResultHandler.handleTournamentResult(gm1, winStatus);

    // Games 2..N (auto-queued by result handler).
    for (int i = 2; i <= gamesNeeded; i++) {
      GameMatch nextGm = getLatestAutoQueuedGame(match);
      tournamentResultHandler.handleTournamentResult(nextGm, winStatus);
    }
  }
}

package org.bytefight.webserver.tournament.application;

import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Handles results for individual games within a tournament series.
 *
 * Path:
 * 1. GameMatchResultHandler detects MatchReason.tournament on a completed GameMatch
 * 2. Calls handleTournamentResult(gameMatch, status)
 * 3. This handler finds the parent TournamentMatch (series) via TournamentGame
 * 4. Updates series win counters based on the game result
 * 5. Decision point:
 *    a) DRAW           -> Queue another game (draws don't count toward series score)
 *    b) Series NOT decided -> Queue the next game (unless 10-game cap reached)
 *    c) Series DECIDED -> Mark series complete, update losses/elimination, advance bracket
 *
 * Series rules:
 * - Bo5: first to 3 game wins takes the series
 * - Bo7: first to 4 game wins takes the series (grand finals only)
 * - Draws queue another game without changing scores
 * - A series is force-resolved after 10 total games:
 *   most wins wins; if tied, the higher seed advances
 * - A series loss counts as ONE bracket loss for the loser (in double-elimination,
 *   two bracket losses = elimination)
 */
@Service
@RequiredArgsConstructor
public class TournamentResultHandler {
    private static final int MAX_SERIES_GAMES = 10;

    private final TournamentMatchRepository tournamentMatchRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final TournamentGameRepository tournamentGameRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentMatchScheduler matchScheduler;
    private final Clock clock;

    /**
     * Applies a single-game result to the parent tournament series.
     *
     * This is the main entry point called by GameMatchResultHandler when a
     * GameMatch with MatchReason.tournament completes.
     *
     * @param gameMatch  the individual GameMatch that completed
     * @param status     the result: team_a_win, team_b_win, or draw
     */
    @Transactional
    public void handleTournamentResult(GameMatch gameMatch, MatchStatus status) {
        // ── Step 1: Look up the series via the TournamentGame join entity ────
        // In the old single-game model, we had TournamentMatch.gameMatch FK.
        // Now we go: GameMatch -> TournamentGame -> TournamentMatch (series).
        TournamentGame tournamentGame = tournamentGameRepository.findByGameMatch(gameMatch).orElseThrow(
                () -> new IllegalStateException("No TournamentGame found for GameMatch " + gameMatch.getUuid())
        );
        TournamentMatch match = tournamentGame.getTournamentMatch();

        if (tournamentGame.isResultProcessed()) {
            return;
        }

        // ── Step 2: Ignore stale/duplicate results ──────────────────────────
        if (match.getState() == TournamentMatchState.COMPLETE
                || match.getState() == TournamentMatchState.SKIPPED) {
            return;
        }

        tournamentGame.setResultProcessed(true);
        tournamentGameRepository.save(tournamentGame);

        TournamentEntry teamOne = match.getTeamOneEntry();
        TournamentEntry teamTwo = match.getTeamTwoEntry();
        if (teamOne == null || teamTwo == null) {
            // Defensive: should not happen in normal scheduling.
            match.setState(TournamentMatchState.SKIPPED);
            tournamentMatchRepository.save(match);
            matchScheduler.processTournament(match.getTournament());
            return;
        }

        int gamesPlayed = tournamentGameRepository.findByTournamentMatchOrderByGameNumberAsc(match).size();

        // ── Step 3: Handle draw (no score change, re-queue another game) ────
        if (status == MatchStatus.draw) {
            if (gamesPlayed >= MAX_SERIES_GAMES) {
                completeSeriesWithCapTiebreak(match, teamOne, teamTwo);
            } else {
                // Draws don't increment either side's series wins.
                // Queue a fresh game at the next game number until the cap.
                matchScheduler.queueSeriesGame(match);
            }
            return;
        }

        // ── Step 4: Increment series wins ───────────────────────────────────
        if (status == MatchStatus.team_a_win) {
            match.setTeamOneSeriesWins(match.getTeamOneSeriesWins() + 1);
        } else if (status == MatchStatus.team_b_win) {
            match.setTeamTwoSeriesWins(match.getTeamTwoSeriesWins() + 1);
        } else {
            throw new IllegalArgumentException("Tournament game received invalid status: " + status);
        }
        tournamentMatchRepository.save(match);

        // ── Step 5: Check if the series is decided ──────────────────────────
        if (match.isSeriesDecided()) {
            completeSeriesByScore(match, teamOne, teamTwo);
            return;
        }

        if (gamesPlayed >= MAX_SERIES_GAMES) {
            completeSeriesWithCapTiebreak(match, teamOne, teamTwo);
        } else {
            // Series still in progress — queue the next game.
            matchScheduler.queueSeriesGame(match);
        }
    }

    private void completeSeriesByScore(TournamentMatch match, TournamentEntry teamOne, TournamentEntry teamTwo) {
        TournamentEntry winner = match.getTeamOneSeriesWins() >= match.getWinsRequired() ? teamOne : teamTwo;
        TournamentEntry loser = winner.equals(teamOne) ? teamTwo : teamOne;
        completeSeries(match, winner, loser);
    }

    private void completeSeriesWithCapTiebreak(TournamentMatch match, TournamentEntry teamOne, TournamentEntry teamTwo) {
        TournamentEntry winner;
        if (match.getTeamOneSeriesWins() > match.getTeamTwoSeriesWins()) {
            winner = teamOne;
        } else if (match.getTeamTwoSeriesWins() > match.getTeamOneSeriesWins()) {
            winner = teamTwo;
        } else {
            winner = pickHigherSeed(teamOne, teamTwo);
        }
        TournamentEntry loser = winner.equals(teamOne) ? teamTwo : teamOne;
        completeSeries(match, winner, loser);
    }

    /**
     * Tournament seeding follows "1 is the top seed", so lower numeric seed wins ties.
     */
    private TournamentEntry pickHigherSeed(TournamentEntry teamOne, TournamentEntry teamTwo) {
        Integer seedOne = teamOne.getSeed();
        Integer seedTwo = teamTwo.getSeed();
        if (seedOne == null && seedTwo == null) {
            return teamOne;
        }
        if (seedOne == null) {
            return teamTwo;
        }
        if (seedTwo == null) {
            return teamOne;
        }
        if (!seedOne.equals(seedTwo)) {
            return seedOne < seedTwo ? teamOne : teamTwo;
        }
        return teamOne;
    }

    private void completeSeries(TournamentMatch match, TournamentEntry winner, TournamentEntry loser) {
        match.setWinnerEntry(winner);
        match.setLoserEntry(loser);
        match.setState(TournamentMatchState.COMPLETE);
        tournamentMatchRepository.save(match);

        // ── Step 7: Update bracket-level losses and elimination ─────────────
        // A series loss counts as ONE bracket loss in double elimination.
        loser.setLosses(loser.getLosses() + 1);
        if (loser.getLosses() >= 2) {
            loser.setStatus(TournamentEntryStatus.ELIMINATED);
            loser.setEliminatedAt(LocalDateTime.now(clock));
        }
        tournamentEntryRepository.save(loser);

        // ── Step 8: Grand final special-case logic ──────────────────────────
        boolean allowLoserAdvance = shouldAllowLoserAdvance(match, winner);
        handleGrandFinal(match, winner, loser);

        // ── Step 9: Advance winner and (if applicable) loser ────────────────
        TournamentEntry loserToAdvance = (loser.getStatus() == TournamentEntryStatus.ELIMINATED || !allowLoserAdvance)
                ? null
                : loser;
        matchScheduler.advanceFromCompletedMatch(match, winner, loserToAdvance);
        matchScheduler.processTournament(match.getTournament());
    }

    // ── Grand final helpers ─────────────────────────────────────────────────

    /**
     * Determines whether the loser should advance to the next bracket node.
     * In the grand final, the winners-bracket champion (teamOneEntry by convention)
     * gets one extra life — if they lose, a grand-final reset is scheduled.
     */
    private boolean shouldAllowLoserAdvance(TournamentMatch match, TournamentEntry winner) {
        if (match.getBracketType() != TournamentBracketType.GRAND_FINAL) {
            return true;
        }
        // If the losers-bracket champion (teamTwoEntry) won the grand final,
        // the winners-bracket champion (teamOneEntry) lost but still has one life.
        return !winner.equals(match.getTeamOneEntry());
    }

    /**
     * Special-case logic for grand finals:
     * - If winners-bracket champion wins grand final -> tournament complete
     * - If winners-bracket champion loses grand final -> schedule grand-final reset (Bo7)
     * - Grand-final reset winner -> tournament complete
     */
    private void handleGrandFinal(TournamentMatch match, TournamentEntry winner, TournamentEntry loser) {
        if (match.getBracketType() == TournamentBracketType.GRAND_FINAL) {
            Tournament tournament = match.getTournament();
            // Verify that the winner has not lost even once in the tournament
            // (i.e., they came through the winners bracket undefeated).
            if (winner.getLosses() == 0) {
                // Winners-bracket champion wins: tournament is complete.
                completeTournament(tournament, winner, loser);
                // Skip the reset match since it's no longer needed.
                tournamentMatchRepository.findByTournamentAndBracketType(tournament, TournamentBracketType.GRAND_FINAL_RESET)
                        .ifPresent(reset -> {
                            if (reset.getState() == TournamentMatchState.PENDING) {
                                reset.setState(TournamentMatchState.SKIPPED);
                                tournamentMatchRepository.save(reset);
                            }
                        });
            } else {
                // Winners-bracket champion lost once: schedule grand-final reset (Bo7).
                TournamentMatch reset = tournamentMatchRepository
                        .findByTournamentAndBracketType(tournament, TournamentBracketType.GRAND_FINAL_RESET)
                        .orElseThrow();
                reset.setTeamOneEntry(match.getTeamOneEntry());
                reset.setTeamTwoEntry(match.getTeamTwoEntry());
                reset.setState(TournamentMatchState.PENDING);
                // Reset match already has seriesLength = 7 from the bracket builder.
                tournamentMatchRepository.save(reset);
            }
        } else if (match.getBracketType() == TournamentBracketType.GRAND_FINAL_RESET) {
            // Reset series winner is the final champion.
            completeTournament(match.getTournament(), winner, loser);
        }
    }

    /**
     * Marks the tournament as COMPLETE and records final standings.
     *
     * @param tournament  the tournament to complete
     * @param champion    the series winner (1st place)
     * @param runnerUp    the series loser (2nd place)
     */
    private void completeTournament(Tournament tournament, TournamentEntry champion, TournamentEntry runnerUp) {
        tournament.setStatus(TournamentStatus.COMPLETE);
        tournament.setFinishedAt(LocalDateTime.now(clock));
        tournament.setFirstPlaceEntry(champion);
        tournament.setSecondPlaceEntry(runnerUp);
        tournamentRepository.save(tournament);
    }
}

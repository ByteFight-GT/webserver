package org.bytefight.webserver.tournament.application;

import org.bytefight.webserver.gameMatch.domain.GameMatch;
import org.bytefight.webserver.gameMatch.domain.MATCH_STATUS;
import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.tournament.domain.TournamentBracketType;
import org.bytefight.webserver.tournament.domain.TournamentEntry;
import org.bytefight.webserver.tournament.domain.TournamentEntryStatus;
import org.bytefight.webserver.tournament.domain.TournamentMatch;
import org.bytefight.webserver.tournament.domain.TournamentMatchState;
import org.bytefight.webserver.tournament.domain.TournamentStatus;
import org.bytefight.webserver.tournament.infra.TournamentEntryRepository;
import org.bytefight.webserver.tournament.infra.TournamentMatchRepository;
import org.bytefight.webserver.tournament.infra.TournamentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Handles results for tournament GameMatch outcomes.
 *
 * Path:
 * - GameMatchResultHandler detects MATCH_REASON.TOURNAMENT
 * - TournamentResultHandler finds TournamentMatch by GameMatch
 * - Updates winner/loser, loss counts, elimination, and downstream slots
 * - Triggers scheduling of any newly-ready matches
 */
@Service
@RequiredArgsConstructor
public class TournamentResultHandler {
    private final TournamentMatchRepository tournamentMatchRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentMatchScheduler matchScheduler;
    private final Clock clock;

    /**
     * Applies a GameMatch result to the tournament bracket graph.
     *
     * Rules:
     * - Draws re-queue the same match later
     * - Loser gains a loss; eliminated on second loss
     * - Winners and (if applicable) losers advance to next matches
     * - Grand final supports reset if winners-bracket champion loses once
     */
    @Transactional
    public void handleTournamentResult(GameMatch gameMatch, MATCH_STATUS status) {
        TournamentMatch tournamentMatch = tournamentMatchRepository.findByGameMatch(gameMatch).orElseThrow();
        if (tournamentMatch.getState() == TournamentMatchState.COMPLETE || tournamentMatch.getState() == TournamentMatchState.SKIPPED) {
            return;
        }

        if (status == MATCH_STATUS.DRAW) {
            tournamentMatch.setGameMatch(null);
            tournamentMatch.setState(TournamentMatchState.PENDING);
            tournamentMatchRepository.save(tournamentMatch);
            matchScheduler.processTournament(tournamentMatch.getTournament());
            return;
        }

        TournamentEntry teamOne = tournamentMatch.getTeamOneEntry();
        TournamentEntry teamTwo = tournamentMatch.getTeamTwoEntry();
        if (teamOne == null || teamTwo == null) {
            tournamentMatch.setState(TournamentMatchState.SKIPPED);
            tournamentMatchRepository.save(tournamentMatch);
            matchScheduler.processTournament(tournamentMatch.getTournament());
            return;
        }

        TournamentEntry winner;
        TournamentEntry loser;
        if (status == MATCH_STATUS.TEAM_ONE_WIN) {
            winner = teamOne;
            loser = teamTwo;
        } else if (status == MATCH_STATUS.TEAM_TWO_WIN) {
            winner = teamTwo;
            loser = teamOne;
        } else {
            throw new IllegalArgumentException("Tournament match received invalid status: " + status);
        }

        tournamentMatch.setWinnerEntry(winner);
        tournamentMatch.setLoserEntry(loser);
        tournamentMatch.setState(TournamentMatchState.COMPLETE);
        tournamentMatchRepository.save(tournamentMatch);

        loser.setLosses(loser.getLosses() + 1);
        if (loser.getLosses() >= 2) {
            loser.setStatus(TournamentEntryStatus.ELIMINATED);
            loser.setEliminatedAt(LocalDateTime.now(clock));
        }
        tournamentEntryRepository.save(loser);

        boolean allowLoserAdvance = shouldAllowLoserAdvance(tournamentMatch, winner);
        handleGrandFinal(tournamentMatch, winner, loser);

        TournamentEntry loserToAdvance = loser.getStatus() == TournamentEntryStatus.ELIMINATED || !allowLoserAdvance ? null : loser;
        matchScheduler.advanceFromCompletedMatch(tournamentMatch, winner, loserToAdvance);
        matchScheduler.processTournament(tournamentMatch.getTournament());
    }

    private boolean shouldAllowLoserAdvance(TournamentMatch tournamentMatch, TournamentEntry winner) {
        if (tournamentMatch.getBracketType() != TournamentBracketType.GRAND_FINAL) {
            return true;
        }
        return !winner.equals(tournamentMatch.getTeamOneEntry());
    }

    /**
     * Special-case logic for grand finals:
     * - If winners-bracket champion wins, tournament completes
     * - If winners-bracket champion loses, schedule grand-final reset
     */
    private void handleGrandFinal(TournamentMatch tournamentMatch, TournamentEntry winner, TournamentEntry loser) {
        if (tournamentMatch.getBracketType() == TournamentBracketType.GRAND_FINAL) {
            Tournament tournament = tournamentMatch.getTournament();
            if (winner.equals(tournamentMatch.getTeamOneEntry())) {
                tournament.setStatus(TournamentStatus.COMPLETE);
                tournament.setFinishedAt(LocalDateTime.now(clock));
                tournamentRepository.save(tournament);
            } else {
                TournamentMatch reset = tournamentMatchRepository
                        .findByTournamentAndBracketType(tournament, TournamentBracketType.GRAND_FINAL_RESET)
                        .orElseThrow();
                reset.setTeamOneEntry(tournamentMatch.getTeamOneEntry());
                reset.setTeamTwoEntry(tournamentMatch.getTeamTwoEntry());
                reset.setState(TournamentMatchState.PENDING);
                tournamentMatchRepository.save(reset);
            }
        } else if (tournamentMatch.getBracketType() == TournamentBracketType.GRAND_FINAL_RESET) {
            Tournament tournament = tournamentMatch.getTournament();
            tournament.setStatus(TournamentStatus.COMPLETE);
            tournament.setFinishedAt(LocalDateTime.now(clock));
            tournamentRepository.save(tournament);
        }
    }
}

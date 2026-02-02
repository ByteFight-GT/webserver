package org.bytefight.webserver.tournament.application;

import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.tournament.domain.TournamentEntry;
import org.bytefight.webserver.tournament.domain.TournamentMatch;
import org.bytefight.webserver.tournament.domain.TournamentMatchState;
import org.bytefight.webserver.tournament.infra.TournamentMatchRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Responsible for moving the bracket forward:
 * - Auto-advance byes (single-team matches)
 * - Queue matches when both sides are present
 * - Place winners/losers into downstream bracket nodes
 *
 * This is the bridge between tournament bracket graph and GameMatch queueing.
 *
 * Competition-aware behavior:
 * - Only teams from the tournament's competition should appear here
 * - Match creation uses MatchReason.tournament and ladder "tournament"
 */
@Service
@RequiredArgsConstructor
public class TournamentMatchScheduler {
    private final TournamentMatchRepository tournamentMatchRepository;
    private final GameMatchService gameMatchService;

    /**
     * Processes the tournament graph until no automatic changes remain.
     *
     * Steps:
     * - Mark empty matches as SKIPPED
     * - Auto-advance byes to the next winner slot
     * - Queue any matches with two participants
     */
    @Transactional
    public void processTournament(Tournament tournament) {
        boolean changed;
        do {
            changed = false;
            List<TournamentMatch> matches = tournamentMatchRepository
                    .findByTournamentOrderByBracketTypeAscRoundNumberAscMatchIndexAsc(tournament);
            for (TournamentMatch match : matches) {
                if (match.getState() != TournamentMatchState.PENDING) {
                    // Only process matches that haven't been queued or completed.
                    continue;
                }
                if (match.getTeamOneEntry() == null && match.getTeamTwoEntry() == null) {
                    // Empty node (no teams feed into it). Mark skipped and move on.
                    match.setState(TournamentMatchState.SKIPPED);
                    tournamentMatchRepository.save(match);
                    changed = true;
                    continue;
                }
                if (match.getTeamOneEntry() == null || match.getTeamTwoEntry() == null) {
                    // Bye scenario: single team auto-advances.
                    TournamentEntry winner = match.getTeamOneEntry() != null ? match.getTeamOneEntry() : match.getTeamTwoEntry();
                    match.setWinnerEntry(winner);
                    match.setState(TournamentMatchState.SKIPPED);
                    tournamentMatchRepository.save(match);
                    advanceWinner(match, winner);
                    changed = true;
                }
            }
        } while (changed);

        // Queue any remaining matches that now have two participants.
        List<TournamentMatch> pending = tournamentMatchRepository.findByTournamentAndState(tournament, TournamentMatchState.PENDING);
        for (TournamentMatch match : pending) {
            if (match.getTeamOneEntry() != null && match.getTeamTwoEntry() != null) {
                queueMatch(match);
            }
        }
    }

    /**
     * Advances entries based on a completed tournament match.
     * Winners go to next winner slot; losers go to loser slot (if any).
     */
    @Transactional
    public void advanceFromCompletedMatch(TournamentMatch match, TournamentEntry winner, TournamentEntry loser) {
        advanceWinner(match, winner);
        advanceLoser(match, loser);
    }

    private void advanceWinner(TournamentMatch match, TournamentEntry winner) {
        placeEntry(match.getNextWinnerMatchId(), match.getNextWinnerSlot(), winner);
    }

    private void advanceLoser(TournamentMatch match, TournamentEntry loser) {
        placeEntry(match.getNextLoserMatchId(), match.getNextLoserSlot(), loser);
    }

    private void placeEntry(Long nextMatchId, Integer slot, TournamentEntry entry) {
        if (nextMatchId == null || slot == null || entry == null) {
            return;
        }
        TournamentMatch target = tournamentMatchRepository.findById(nextMatchId).orElseThrow();
        if (slot == 1) {
            // Slot 1 corresponds to teamOneEntry in the target match.
            if (target.getTeamOneEntry() == null) {
                target.setTeamOneEntry(entry);
            }
        } else {
            // Slot 2 corresponds to teamTwoEntry in the target match.
            if (target.getTeamTwoEntry() == null) {
                target.setTeamTwoEntry(entry);
            }
        }
        tournamentMatchRepository.save(target);
    }

    /**
     * Creates and queues a GameMatch for a tournament match.
     * Uses MatchReason.tournament so results route back here.
     */
    private void queueMatch(TournamentMatch match) {
        if (match.getGameMatch() != null) {
            // Already queued/linked.
            return;
        }
        Team teamOne = match.getTeamOneEntry().getTeam();
        Team teamTwo = match.getTeamTwoEntry().getTeam();
        if (!teamOne.getCompetition().equals(match.getTournament().getCompetition())
                || !teamTwo.getCompetition().equals(match.getTournament().getCompetition())) {
            throw new IllegalArgumentException("Tournament match teams must belong to the tournament competition.");
        }
        Submission submissionOne = teamOne.getCurrentSubmission();
        Submission submissionTwo = teamTwo.getCurrentSubmission();
        if (submissionOne == null || submissionTwo == null) {
            // Tournament rules require an active submission for both teams.
            throw new IllegalArgumentException("Tournament match cannot be queued without current submissions.");
        }
        // Create a GameMatch tied to this tournament match.
        GameMatch gameMatch = gameMatchService.createMatch(
                null,
                teamOne,
                teamTwo,
                submissionOne,
                submissionTwo,
                "tournament",
                MatchReason.tournament,
                null
        );
        // Scheduling pushes the match job to RabbitMQ.
        gameMatchService.scheduleMatch(gameMatch);
        match.setGameMatch(gameMatch);
        match.setState(TournamentMatchState.QUEUED);
        tournamentMatchRepository.save(match);
    }
}

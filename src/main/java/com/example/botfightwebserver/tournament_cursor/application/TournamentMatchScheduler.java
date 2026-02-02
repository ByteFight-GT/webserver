package com.example.botfightwebserver.tournament_cursor.application;

import com.example.botfightwebserver.gameMatch.application.GameMatchService;
import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.gameMatch.domain.MATCH_REASON;
import com.example.botfightwebserver.submission.domain.Submission;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.tournament_cursor.domain.Tournament;
import com.example.botfightwebserver.tournament_cursor.domain.TournamentEntry;
import com.example.botfightwebserver.tournament_cursor.domain.TournamentMatch;
import com.example.botfightwebserver.tournament_cursor.domain.TournamentMatchState;
import com.example.botfightwebserver.tournament_cursor.infra.TournamentMatchRepository;
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
                    continue;
                }
                if (match.getTeamOneEntry() == null && match.getTeamTwoEntry() == null) {
                    match.setState(TournamentMatchState.SKIPPED);
                    tournamentMatchRepository.save(match);
                    changed = true;
                    continue;
                }
                if (match.getTeamOneEntry() == null || match.getTeamTwoEntry() == null) {
                    TournamentEntry winner = match.getTeamOneEntry() != null ? match.getTeamOneEntry() : match.getTeamTwoEntry();
                    match.setWinnerEntry(winner);
                    match.setState(TournamentMatchState.SKIPPED);
                    tournamentMatchRepository.save(match);
                    advanceWinner(match, winner);
                    changed = true;
                }
            }
        } while (changed);

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
            if (target.getTeamOneEntry() == null) {
                target.setTeamOneEntry(entry);
            }
        } else {
            if (target.getTeamTwoEntry() == null) {
                target.setTeamTwoEntry(entry);
            }
        }
        tournamentMatchRepository.save(target);
    }

    /**
     * Creates and queues a GameMatch for a tournament match.
     * Uses MATCH_REASON.TOURNAMENT so results route back here.
     */
    private void queueMatch(TournamentMatch match) {
        if (match.getGameMatch() != null) {
            return;
        }
        Team teamOne = match.getTeamOneEntry().getTeam();
        Team teamTwo = match.getTeamTwoEntry().getTeam();
        Submission submissionOne = teamOne.getCurrentSubmission();
        Submission submissionTwo = teamTwo.getCurrentSubmission();
        if (submissionOne == null || submissionTwo == null) {
            throw new IllegalArgumentException("Tournament match cannot be queued without current submissions.");
        }
        GameMatch gameMatch = gameMatchService.createMatch(
                teamOne.getUuid().toString(),
                teamTwo.getUuid().toString(),
                submissionOne.getUuid().toString(),
                submissionTwo.getUuid().toString(),
                MATCH_REASON.TOURNAMENT
        );
        gameMatchService.queueMatch(gameMatch);
        match.setGameMatch(gameMatch);
        match.setState(TournamentMatchState.QUEUED);
        tournamentMatchRepository.save(match);
    }
}

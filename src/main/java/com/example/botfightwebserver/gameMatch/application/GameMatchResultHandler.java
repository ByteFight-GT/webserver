package com.example.botfightwebserver.gameMatch.application;


import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.gameMatch.domain.GameMatchResult;
import com.example.botfightwebserver.gameMatch.domain.MatchReason;
import com.example.botfightwebserver.gameMatch.domain.MatchStatus;
import com.example.botfightwebserver.gameMatchLogs.GameMatchLogService;
import com.example.botfightwebserver.glicko.GlickoCalculator;
import com.example.botfightwebserver.glicko.GlickoChanges;
import com.example.botfightwebserver.glicko.GlickoHistoryService;
import com.example.botfightwebserver.rabbitMQ.application.RabbitMQService;
import com.example.botfightwebserver.submission.domain.Submission;
import com.example.botfightwebserver.submission.application.SubmissionService;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.tournament.TournamentGameMatch;
import com.example.botfightwebserver.tournament.TournamentGameMatchService;
import com.example.botfightwebserver.tournament.TournamentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
@Slf4j
public class GameMatchResultHandler {

    private final GameMatchService gameMatchService;
    private final TeamService teamService;
    private final SubmissionService submissionService;
    private final RabbitMQService rabbitMQService;
    private final GlickoCalculator glickoCalculator;
    private final GameMatchLogService gameMatchLogService;
    private final TournamentService tournamentService;
    private final TournamentGameMatchService tournamentGameMatchService;
    private final GlickoHistoryService glickoHistoryService;


    public void handleGameMatchResult(GameMatchResult result) {
        String gameMatchUuid = result.matchUuid();
        if (!gameMatchService.isGameMatchUuidExist(gameMatchUuid)) {
            throw new IllegalArgumentException("Game match id " + gameMatchUuid + " does not exist");
        }
        if (!gameMatchService.isGameMatchWaiting(gameMatchUuid)) {
            throw new UnsupportedOperationException("Game match is already played {}" + result);
        }
        MatchStatus status = result.status();
        GameMatch gameMatch = gameMatchService.getReferenceByUuid(gameMatchUuid).orElseThrow();
        Team team1 = gameMatch.getTeamOne();
        Team team2 = gameMatch.getTeamTwo();

        log.info("Processing match result for game {}: {} vs {}, status: {}",
                gameMatchUuid, team1.getName(), team2.getName(), status);

        GlickoChanges glickoChanges = new GlickoChanges();
        if (gameMatch.getReason() == MatchReason.LADDER) {
            glickoChanges = glickoCalculator.calculateGlicko(team1, team2, status);
            log.info("Handling ladder match: team1 {}, team2 {}", team1.getId(), team2.getId());
            handleLadderResult(team1, team2, status, glickoChanges, gameMatch);
            log.info("Ladder match handled");
        } else if (gameMatch.getReason() == MatchReason.VALIDATION) {
            Submission submission = gameMatch.getSubmissionOne();
            log.info("Processing validation match for team {} and submission {}", team1.getId(), submission.getId());
            handleValidationResult(team1, submission, status);
            log.info("Validation match handled");
        } else if (gameMatch.getReason() == MatchReason.TOURNAMENT) {
            log.info("Processing tournament match for team1 {} team2 {}", team1.getId(), team2.getId());
            handleTournamentResult(gameMatch.getId(), status, team1, team2);
        } else if (gameMatch.getReason() == MatchReason.SCRIMMAGE) {
            log.info("Processing Scrimmage match for team1 {} team2 {}", team1.getId(), team2.getId());
            handleScrimmageResult(team1, team2, status);
        }
        else {
            log.info("Can't process match");
        }
        gameMatchService.setGameMatchStatus(gameMatchUuid, status);
        gameMatchLogService.createGameMatchLog(gameMatch, result.matchLog(), glickoChanges.getTeam1Change(), glickoChanges.getTeam2Change());
    }

    private void handleLadderResult(Team team1, Team team2, MatchStatus status, GlickoChanges glickoChanges, GameMatch gameMatch) {
        updateTeamStats(team1, team2, status, glickoChanges);
        glickoHistoryService.save(team1, gameMatch);
        glickoHistoryService.save(team2, gameMatch);
    }

    private void handleScrimmageResult(Team team1, Team team2, MatchStatus status) {
        updateTeamStats(team1, team2, status, new GlickoChanges());
    }

    private void updateTeamStats(Team team1, Team team2, MatchStatus status, GlickoChanges glickoChanges) {
        if (status == MatchStatus.TEAM_ONE_WIN) {
            teamService.updateAfterMatch(team1, glickoChanges.getTeam1Change(), glickoChanges.getTeam1PhiChange(),glickoChanges.getTeam1SigmaChange(), true, false);
            teamService.updateAfterMatch(team2, glickoChanges.getTeam2Change(), glickoChanges.getTeam2PhiChange(), glickoChanges.getTeam2SigmaChange(), false, false);
        } else if (status == MatchStatus.TEAM_TWO_WIN) {
            teamService.updateAfterMatch(team1, glickoChanges.getTeam1Change(), glickoChanges.getTeam1PhiChange(),glickoChanges.getTeam1SigmaChange(), false, false);
            teamService.updateAfterMatch(team2, glickoChanges.getTeam2Change(), glickoChanges.getTeam2PhiChange(), glickoChanges.getTeam2SigmaChange(), true, false);
        } else if (status == MatchStatus.DRAW) {
            teamService.updateAfterMatch(team1, glickoChanges.getTeam1Change(), glickoChanges.getTeam1PhiChange(),glickoChanges.getTeam1SigmaChange(), false, true);
            teamService.updateAfterMatch(team2, glickoChanges.getTeam2Change(), glickoChanges.getTeam2PhiChange(), glickoChanges.getTeam2SigmaChange(), false, true);
        }
    }

    private void handleTournamentResult(Long gameMatchId, MatchStatus status, Team team1, Team team2) {
        TournamentGameMatch tournamentGameMatch = tournamentGameMatchService.findById(gameMatchId);
        tournamentService.updateMatchResult(gameMatchId, status);
//        updateTeamStats(team1, team2, status, new GlickoChanges());
        tournamentGameMatchService.save(tournamentGameMatch);
    }


    public void submitGameMatchResults(GameMatchResult result) {
        if (!gameMatchService.isGameMatchUuidExist(result.matchUuid())) {
            throw new RuntimeException("Game match id " + result.matchUuid() + " does not exist");
        }
        rabbitMQService.enqueueGameMatchResult(result);
    }

    private  void handleValidationResult(Team team, Submission submission, MatchStatus status) {
        if (status == MatchStatus.TEAM_ONE_WIN) {
            submissionService.validateSubmissionAfterMatch(submission.getId());
            if (teamService.getCurrentSubmission(team.getId()).isEmpty() || submission.getIsAutoSet()) {
                teamService.setCurrentSubmission(team.getId(), submission.getUuid().toString());
            }
        } else {
            submissionService.invalidateSubmissionAfterMatch(submission.getId());
        }
    }

    public List<GameMatchResult> deleteQueuedMatches() {
        List<GameMatchResult> removedResults = rabbitMQService.deleteGameResultQueue();
        return removedResults;
    }
}

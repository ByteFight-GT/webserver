package com.example.botfightwebserver.gameMatchResult;


import com.example.botfightwebserver.gameMatch.GameMatch;
import com.example.botfightwebserver.gameMatch.GameMatchService;
import com.example.botfightwebserver.gameMatch.MATCH_REASON;
import com.example.botfightwebserver.gameMatch.MATCH_STATUS;
import com.example.botfightwebserver.gameMatchLogs.GameMatchLogService;
import com.example.botfightwebserver.glicko.GlickoCalculator;
import com.example.botfightwebserver.glicko.GlickoChanges;
import com.example.botfightwebserver.rabbitMQ.RabbitMQService;
import com.example.botfightwebserver.submission.Submission;
import com.example.botfightwebserver.submission.SubmissionService;
import com.example.botfightwebserver.team.Team;
import com.example.botfightwebserver.team.TeamService;
import com.example.botfightwebserver.tournament.TOURNAMENT_SET_STATES;
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


    public void handleGameMatchResult(GameMatchResult result) {
        long gameMatchId = result.matchId();
        if (!gameMatchService.isGameMatchIdExist(gameMatchId)) {
            throw new IllegalArgumentException("Game match id " + gameMatchId + " does not exist");
        }
        if (!gameMatchService.isGameMatchWaiting(gameMatchId)) {
            throw new UnsupportedOperationException("Game match is already played {}" + result);
        }
        MATCH_STATUS status = result.status();
        GameMatch gameMatch = gameMatchService.getReferenceById(gameMatchId);
        Team team1 = gameMatch.getTeamOne();
        Team team2 = gameMatch.getTeamTwo();

        log.info("Processing match result for game {}: {} vs {}, status: {}",
            gameMatchId, team1.getName(), team2.getName(), status);

        GlickoChanges glickoChanges = new GlickoChanges();
        if (gameMatch.getReason() == MATCH_REASON.LADDER) {
            glickoChanges = glickoCalculator.calculateGlicko(team1, team2, status);
            log.info("Handling ladder match: team1 {}, team2 {}", team1.getId(), team2.getId());
            handleLadderResult(team1, team2, status, glickoChanges);
            log.info("Ladder match handled");
        } else if (gameMatch.getReason() == MATCH_REASON.VALIDATION) {
            Submission submission = gameMatch.getSubmissionOne();
            log.info("Processing validation match for team {} and submission {}", team1.getId(), submission.getId());
            handleValidationResult(team1, submission, status);
            log.info("Validation match handled");
        } else if (gameMatch.getReason() == MATCH_REASON.TOURNAMENT) {
            log.info("Processing tournament match for team1 {} team2 {}", team1.getId(), team2.getId());
            handleTournamentResult(gameMatch.getId(), status, team1, team2);
        } else if (gameMatch.getReason() == MATCH_REASON.SCRIMMAGE) {
            log.info("Processing Scrimmage match for team1 {} team2 {}", team1.getId(), team2.getId());
            handleScrimmageResult(team1, team2, status);
        }
        else {
            log.info("Can't process match");
        }
        gameMatchService.setGameMatchStatus(gameMatchId, status);
        gameMatchLogService.createGameMatchLog(gameMatch, result.matchLog(), glickoChanges.getTeam1Change(), glickoChanges.getTeam2Change());
    }

    private void handleLadderResult(Team team1, Team team2, MATCH_STATUS status, GlickoChanges glickoChanges) {
        updateTeamStats(team1, team2, status, glickoChanges);
    }

    private void handleScrimmageResult(Team team1, Team team2, MATCH_STATUS status) {
        updateTeamStats(team1, team2, status, new GlickoChanges());
    }

    private void updateTeamStats(Team team1, Team team2, MATCH_STATUS status, GlickoChanges glickoChanges) {
        if (status == MATCH_STATUS.TEAM_ONE_WIN) {
            teamService.updateAfterMatch(team1, glickoChanges.getTeam1Change(), glickoChanges.getTeam1PhiChange(),glickoChanges.getTeam1SigmaChange(), true, false);
            teamService.updateAfterMatch(team2, glickoChanges.getTeam2Change(), glickoChanges.getTeam2PhiChange(), glickoChanges.getTeam2SigmaChange(), false, false);
        } else if (status == MATCH_STATUS.TEAM_TWO_WIN) {
            teamService.updateAfterMatch(team1, glickoChanges.getTeam1Change(), glickoChanges.getTeam1PhiChange(),glickoChanges.getTeam1SigmaChange(), false, false);
            teamService.updateAfterMatch(team2, glickoChanges.getTeam2Change(), glickoChanges.getTeam2PhiChange(), glickoChanges.getTeam2SigmaChange(), true, false);
        } else if (status == MATCH_STATUS.DRAW) {
            teamService.updateAfterMatch(team1, glickoChanges.getTeam1Change(), glickoChanges.getTeam1PhiChange(),glickoChanges.getTeam1SigmaChange(), false, true);
            teamService.updateAfterMatch(team2, glickoChanges.getTeam2Change(), glickoChanges.getTeam2PhiChange(), glickoChanges.getTeam2SigmaChange(), false, true);
        }
    }

    private void handleTournamentResult(Long gameMatchId, MATCH_STATUS status, Team team1, Team team2) {
        TournamentGameMatch tournamentGameMatch = tournamentGameMatchService.findById(gameMatchId);
        tournamentService.updateMatchResult(gameMatchId, status);
//        updateTeamStats(team1, team2, status, new GlickoChanges());
        tournamentGameMatchService.save(tournamentGameMatch);
    }


    public void submitGameMatchResults(GameMatchResult result) {
        if (!gameMatchService.isGameMatchIdExist(result.matchId())) {
            throw new RuntimeException("Game match id " + result.matchId() + " does not exist");
        }
        rabbitMQService.enqueueGameMatchResult(result);
    }

    private  void handleValidationResult(Team team, Submission submission, MATCH_STATUS status) {
        if (status == MATCH_STATUS.TEAM_ONE_WIN) {
            submissionService.validateSubmissionAfterMatch(submission.getId());
            if (teamService.getCurrentSubmission(team.getId()).isEmpty() || submission.getIsAutoSet()) {
                teamService.setCurrentSubmission(team.getId(), submission.getId());
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

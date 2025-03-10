package com.example.botfightwebserver.gameMatchResult;

import com.example.botfightwebserver.glicko.GlickoCalculator;
import com.example.botfightwebserver.glicko.GlickoChanges;
import com.example.botfightwebserver.gameMatch.GameMatch;
import com.example.botfightwebserver.gameMatch.GameMatchService;
import com.example.botfightwebserver.gameMatch.MATCH_REASON;
import com.example.botfightwebserver.gameMatch.MATCH_STATUS;
import com.example.botfightwebserver.gameMatchLogs.GameMatchLogService;
import com.example.botfightwebserver.team.Team;
import com.example.botfightwebserver.team.TeamService;
import com.example.botfightwebserver.rabbitMQ.RabbitMQService;
import com.example.botfightwebserver.submission.Submission;
import com.example.botfightwebserver.submission.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameMatchResultHandlerTest {

    @Mock
    private GameMatchService gameMatchService;
    @Mock
    private TeamService teamService;
    @Mock
    private SubmissionService submissionService;
    @Mock
    private RabbitMQService rabbitMQService;
    @Mock
    private GlickoCalculator glickoCalculator;
    @Mock
    private GameMatchLogService gameMatchLogService;

    private GameMatchResultHandler gameMatchResultHandler;
    private GameMatch gameMatch;
    private Team team1;
    private Team team2;
    private Submission submission;

    @BeforeEach
    void setUp() {
        gameMatchResultHandler = new GameMatchResultHandler(
            gameMatchService,
            teamService,
            submissionService,
            rabbitMQService,
            glickoCalculator,
            gameMatchLogService
        );

        team1 = new Team();
        team1.setId(1L);
        team1.setName("Team1");

        team2 = new Team();
        team2.setId(2L);
        team2.setName("Team2");

        submission = new Submission();
        submission.setId(1L);

        gameMatch = new GameMatch();
        gameMatch.setId(1L);
        gameMatch.setTeamOne(team1);
        gameMatch.setTeamTwo(team2);
        gameMatch.setSubmissionOne(submission);
    }

    @Test
    void handleGameMatchResult_LadderMatch_TeamOneWin() {
        GameMatchResult result = new GameMatchResult(1L, MATCH_STATUS.TEAM_ONE_WIN, "match log");
        gameMatch.setReason(MATCH_REASON.LADDER);
        GlickoChanges glickoChanges = new GlickoChanges(15.0, -15.0, 0.0, 0.0, 0.0, 0.0);

        when(gameMatchService.isGameMatchIdExist(1L)).thenReturn(true);
        when(gameMatchService.isGameMatchWaiting(1L)).thenReturn(true);
        when(gameMatchService.getReferenceById(1L)).thenReturn(gameMatch);
        when(glickoCalculator.calculateGlicko(team1, team2, MATCH_STATUS.TEAM_ONE_WIN))
            .thenReturn(glickoChanges);

        gameMatchResultHandler.handleGameMatchResult(result);

        verify(teamService).updateAfterMatch(team1, 15,0.0,0.0, true, false);
        verify(teamService).updateAfterMatch(team2, -15, 0.0,0.0, false, false);
        verify(gameMatchService).setGameMatchStatus(1L, MATCH_STATUS.TEAM_ONE_WIN);
        verify(gameMatchLogService).createGameMatchLog(1L, "match log", 15, -15);
    }

    @Test
    void handleGameMatchResult_ValidationMatch() {
        GameMatchResult result = new GameMatchResult(1L, MATCH_STATUS.TEAM_ONE_WIN, "match log");
        gameMatch.setReason(MATCH_REASON.VALIDATION);

        when(gameMatchService.isGameMatchIdExist(1L)).thenReturn(true);
        when(gameMatchService.isGameMatchWaiting(1L)).thenReturn(true);
        when(gameMatchService.getReferenceById(1L)).thenReturn(gameMatch);
        when(teamService.getCurrentSubmission(1L)).thenReturn(Optional.empty());

        gameMatchResultHandler.handleGameMatchResult(result);

        verify(submissionService).validateSubmissionAfterMatch(1L);
        verify(teamService).setCurrentSubmission(1L, 1L);
        verify(gameMatchService).setGameMatchStatus(1L, MATCH_STATUS.TEAM_ONE_WIN);
        verify(gameMatchLogService).createGameMatchLog(1L, "match log", 0, 0);
    }

    @Test
    void handleGameMatchResult_NonExistentMatch() {
        GameMatchResult result = new GameMatchResult(999L, MATCH_STATUS.TEAM_ONE_WIN, "match log");
        when(gameMatchService.isGameMatchIdExist(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
            () -> gameMatchResultHandler.handleGameMatchResult(result));
    }

    @Test
    void handleGameMatchResult_AlreadyPlayedMatch() {
        GameMatchResult result = new GameMatchResult(1L, MATCH_STATUS.TEAM_ONE_WIN, "match log");
        when(gameMatchService.isGameMatchIdExist(1L)).thenReturn(true);
        when(gameMatchService.isGameMatchWaiting(1L)).thenReturn(false);

        assertThrows(UnsupportedOperationException.class,
            () -> gameMatchResultHandler.handleGameMatchResult(result));
    }

    @Test
    void submitGameMatchResults_Success() {
        GameMatchResult result = new GameMatchResult(1L, MATCH_STATUS.TEAM_ONE_WIN, "match log");
        when(gameMatchService.isGameMatchIdExist(1L)).thenReturn(true);

        gameMatchResultHandler.submitGameMatchResults(result);

        verify(rabbitMQService).enqueueGameMatchResult(result);
    }

    @Test
    void submitGameMatchResults_NonExistentMatch() {
        GameMatchResult result = new GameMatchResult(999L, MATCH_STATUS.TEAM_ONE_WIN, "match log");
        when(gameMatchService.isGameMatchIdExist(999L)).thenReturn(false);

        assertThrows(RuntimeException.class,
            () -> gameMatchResultHandler.submitGameMatchResults(result));
    }

    @Test
    void deleteQueuedMatches_Success() {
        List<GameMatchResult> expectedResults = Arrays.asList(
            new GameMatchResult(1L, MATCH_STATUS.TEAM_ONE_WIN, "log1"),
            new GameMatchResult(2L, MATCH_STATUS.TEAM_TWO_WIN, "log2")
        );
        when(rabbitMQService.deleteGameResultQueue()).thenReturn(expectedResults);

        List<GameMatchResult> actualResults = gameMatchResultHandler.deleteQueuedMatches();

        assertEquals(expectedResults, actualResults);
        verify(rabbitMQService).deleteGameResultQueue();
    }
}
package com.example.botfightwebserver.gameMatchResult;

import com.example.botfightwebserver.glicko.GlickoCalculator;
import com.example.botfightwebserver.glicko.GlickoChanges;
import com.example.botfightwebserver.gameMatch.GameMatch;
import com.example.botfightwebserver.gameMatch.GameMatchService;
import com.example.botfightwebserver.gameMatch.MATCH_REASON;
import com.example.botfightwebserver.gameMatch.MATCH_STATUS;
import com.example.botfightwebserver.gameMatchLogs.GameMatchLogService;
import com.example.botfightwebserver.player.Player;
import com.example.botfightwebserver.player.PlayerService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameMatchResultHandlerTest {

    @Mock
    private GameMatchService gameMatchService;
    @Mock
    private PlayerService playerService;
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
    private Player player1;
    private Player player2;
    private Submission submission;

    @BeforeEach
    void setUp() {
        gameMatchResultHandler = new GameMatchResultHandler(
            gameMatchService,
            playerService,
            submissionService,
            rabbitMQService,
            glickoCalculator,
            gameMatchLogService
        );

        player1 = new Player();
        player1.setId(1L);
        player1.setName("Player1");

        player2 = new Player();
        player2.setId(2L);
        player2.setName("Player2");

        submission = new Submission();
        submission.setId(1L);

        gameMatch = new GameMatch();
        gameMatch.setId(1L);
        gameMatch.setPlayerOne(player1);
        gameMatch.setPlayerTwo(player2);
        gameMatch.setSubmissionOne(submission);
    }

    @Test
    void handleGameMatchResult_LadderMatch_PlayerOneWin() {
        GameMatchResult result = new GameMatchResult(1L, MATCH_STATUS.PLAYER_ONE_WIN, "match log");
        gameMatch.setReason(MATCH_REASON.LADDER);
        GlickoChanges glickoChanges = new GlickoChanges(15.0, -15.0, 0.0, 0.0, 0.0, 0.0);

        when(gameMatchService.isGameMatchIdExist(1L)).thenReturn(true);
        when(gameMatchService.isGameMatchWaiting(1L)).thenReturn(true);
        when(gameMatchService.getReferenceById(1L)).thenReturn(gameMatch);
        when(glickoCalculator.calculateGlicko(player1, player2, MATCH_STATUS.PLAYER_ONE_WIN))
            .thenReturn(glickoChanges);

        gameMatchResultHandler.handleGameMatchResult(result);

        verify(playerService).updatePlayerAfterLadderMatch(player1, 15,0.0,0.0, true, false);
        verify(playerService).updatePlayerAfterLadderMatch(player2, -15, 0.0,0.0, false, false);
        verify(gameMatchService).setGameMatchStatus(1L, MATCH_STATUS.PLAYER_ONE_WIN);
        verify(gameMatchLogService).createGameMatchLog(1L, "match log", 15, -15);
    }

    @Test
    void handleGameMatchResult_ValidationMatch() {
        GameMatchResult result = new GameMatchResult(1L, MATCH_STATUS.PLAYER_ONE_WIN, "match log");
        gameMatch.setReason(MATCH_REASON.VALIDATION);

        when(gameMatchService.isGameMatchIdExist(1L)).thenReturn(true);
        when(gameMatchService.isGameMatchWaiting(1L)).thenReturn(true);
        when(gameMatchService.getReferenceById(1L)).thenReturn(gameMatch);
        when(playerService.getCurrentSubmission(1L)).thenReturn(Optional.empty());

        gameMatchResultHandler.handleGameMatchResult(result);

        verify(submissionService).validateSubmissionAfterMatch(1L);
        verify(playerService).setCurrentSubmission(1L, 1L);
        verify(gameMatchService).setGameMatchStatus(1L, MATCH_STATUS.PLAYER_ONE_WIN);
        verify(gameMatchLogService).createGameMatchLog(1L, "match log", 0, 0);
    }

    @Test
    void handleGameMatchResult_NonExistentMatch() {
        GameMatchResult result = new GameMatchResult(999L, MATCH_STATUS.PLAYER_ONE_WIN, "match log");
        when(gameMatchService.isGameMatchIdExist(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
            () -> gameMatchResultHandler.handleGameMatchResult(result));
    }

    @Test
    void handleGameMatchResult_AlreadyPlayedMatch() {
        GameMatchResult result = new GameMatchResult(1L, MATCH_STATUS.PLAYER_ONE_WIN, "match log");
        when(gameMatchService.isGameMatchIdExist(1L)).thenReturn(true);
        when(gameMatchService.isGameMatchWaiting(1L)).thenReturn(false);

        assertThrows(UnsupportedOperationException.class,
            () -> gameMatchResultHandler.handleGameMatchResult(result));
    }

    @Test
    void submitGameMatchResults_Success() {
        GameMatchResult result = new GameMatchResult(1L, MATCH_STATUS.PLAYER_ONE_WIN, "match log");
        when(gameMatchService.isGameMatchIdExist(1L)).thenReturn(true);

        gameMatchResultHandler.submitGameMatchResults(result);

        verify(rabbitMQService).enqueueGameMatchResult(result);
    }

    @Test
    void submitGameMatchResults_NonExistentMatch() {
        GameMatchResult result = new GameMatchResult(999L, MATCH_STATUS.PLAYER_ONE_WIN, "match log");
        when(gameMatchService.isGameMatchIdExist(999L)).thenReturn(false);

        assertThrows(RuntimeException.class,
            () -> gameMatchResultHandler.submitGameMatchResults(result));
    }

    @Test
    void deleteQueuedMatches_Success() {
        List<GameMatchResult> expectedResults = Arrays.asList(
            new GameMatchResult(1L, MATCH_STATUS.PLAYER_ONE_WIN, "log1"),
            new GameMatchResult(2L, MATCH_STATUS.PLAYER_TWO_WIN, "log2")
        );
        when(rabbitMQService.deleteGameResultQueue()).thenReturn(expectedResults);

        List<GameMatchResult> actualResults = gameMatchResultHandler.deleteQueuedMatches();

        assertEquals(expectedResults, actualResults);
        verify(rabbitMQService).deleteGameResultQueue();
    }
}
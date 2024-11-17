package com.example.botfightwebserver.gameMatchLogs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GameMatchLogServiceTest {

    @Mock
    private GameMatchLogRepository gameMatchLogRepository;

    private GameMatchLogService gameMatchLogService;

    @BeforeEach
    void setUp() {
        gameMatchLogService = new GameMatchLogService(gameMatchLogRepository);
    }

    @Test
    void createGameMatchLog_allArgs() {
        // Setup
        Long gameMatchId = 1L;
        String logs = "Test logs";
        double player1EloChange = 10.0;
        double player2EloChange = -10.0;

        GameMatchLog expectedGameMatchLog = GameMatchLog.builder()
                .matchId(gameMatchId)
                .matchLog(logs)
                .player1EloChange(player1EloChange)
                .player2EloChange(player2EloChange)
                .build();

        // Configure mock behavior
        when(gameMatchLogRepository.save(argThat(gameMatchLog ->
                gameMatchLog.getMatchId().equals(gameMatchId) &&
                        gameMatchLog.getMatchLog().equals(logs) &&
                        gameMatchLog.getPlayer1EloChange() == player1EloChange &&
                        gameMatchLog.getPlayer2EloChange() == player2EloChange
        ))).thenReturn(expectedGameMatchLog);

        // Call function
        GameMatchLog result = gameMatchLogService.createGameMatchLog(
                gameMatchId,
                logs,
                player1EloChange,
                player2EloChange
        );

        // Assert
        assertNotNull(result);
        assertEquals(gameMatchId, result.getMatchId());
        assertEquals(logs, result.getMatchLog());
        assertEquals(player1EloChange, result.getPlayer1EloChange());
        assertEquals(player2EloChange, result.getPlayer2EloChange());

        // Verify repository interaction
        verify(gameMatchLogRepository).save(any(GameMatchLog.class));
    }
}
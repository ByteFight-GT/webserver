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
        double team1GlickoChange = 10.0;
        double team2GlickoChange = -10.0;

        GameMatchLog expectedGameMatchLog = GameMatchLog.builder()
                .matchId(gameMatchId)
                .matchLog(logs)
                .team1GlickoChange(team1GlickoChange)
                .team2GlickoChange(team2GlickoChange)
                .build();

        // Configure mock behavior
        when(gameMatchLogRepository.save(argThat(gameMatchLog ->
                gameMatchLog.getMatchId().equals(gameMatchId) &&
                        gameMatchLog.getMatchLog().equals(logs) &&
                        gameMatchLog.getTeam1GlickoChange() == team1GlickoChange &&
                        gameMatchLog.getTeam2GlickoChange() == team2GlickoChange
        ))).thenReturn(expectedGameMatchLog);

        // Call function
        GameMatchLog result = gameMatchLogService.createGameMatchLog(
                gameMatchId,
                logs,
                team1GlickoChange,
                team2GlickoChange
        );

        // Assert
        assertNotNull(result);
        assertEquals(gameMatchId, result.getMatchId());
        assertEquals(logs, result.getMatchLog());
        assertEquals(team1GlickoChange, result.getTeam1GlickoChange());
        assertEquals(team2GlickoChange, result.getTeam2GlickoChange());

        // Verify repository interaction
        verify(gameMatchLogRepository).save(any(GameMatchLog.class));
    }
}
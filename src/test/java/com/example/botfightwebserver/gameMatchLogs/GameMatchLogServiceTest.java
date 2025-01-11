package com.example.botfightwebserver.gameMatchLogs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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

    @Test
    void getAllGameMatchLogs() {
        Long gameMatchId = 1L;
        String logs = "Test logs";
        double team1GlickoChange = 10.0;
        double team2GlickoChange = -10.0;

        GameMatchLog gameMatchLog = GameMatchLog.builder()
            .matchId(gameMatchId)
            .matchLog(logs)
            .team1GlickoChange(team1GlickoChange)
            .team2GlickoChange(team2GlickoChange)
            .build();

        when(gameMatchLogRepository.findAll()).thenReturn(List.of(gameMatchLog));

        List<GameMatchLog> result = gameMatchLogService.getAllGameMatchLogs();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(gameMatchId, result.get(0).getMatchId());
        assertEquals(logs, result.get(0).getMatchLog());
        assertEquals(team1GlickoChange, result.get(0).getTeam1GlickoChange());
        assertEquals(team2GlickoChange, result.get(0).getTeam2GlickoChange());

        verify(gameMatchLogRepository).findAll();
    }

    @Test
    void getGameMatchLogById_shouldReturnLog() {
        GameMatchLog log = new GameMatchLog();
        log.setId(1L);
        when(gameMatchLogRepository.findById(1L)).thenReturn(Optional.of(log));

        Optional<GameMatchLog> result = gameMatchLogService.getGameMatchLogById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(gameMatchLogRepository).findById(1L);
    }

    @Test
    void getGameMatchLogIds_shouldReturnAllIds() {
        GameMatchLog log1 = new GameMatchLog();
        log1.setId(1L);
        GameMatchLog log2 = new GameMatchLog();
        log2.setId(2L);
        when(gameMatchLogRepository.findAll()).thenReturn(List.of(log1, log2));

        List<Long> result = gameMatchLogService.getGameMatchLogIds();

        assertEquals(List.of(1L, 2L), result);
        verify(gameMatchLogRepository).findAll();
    }
}
package com.example.botfightwebserver.gameMatchLogs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameMatchLogController.class)
public class GameMatchLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameMatchLogService gameMatchLogService;

    private static final GameMatchLog gameMatchLog = GameMatchLog.builder()
        .id(2L)
        .matchId(1L)
        .matchLog("matchlogs")
        .team2GlickoChange(100.0)
        .team1GlickoChange(-100.0)
        .build();

    @Test
    void shouldGetAllLogs() throws Exception {
        List<GameMatchLog> logs = List.of(gameMatchLog);
        when(gameMatchLogService.getAllGameMatchLogs()).thenReturn(logs);

        mockMvc.perform(get("/api/v1/game-match-log/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(2L))
            .andExpect(jsonPath("$[0].matchId").value(1L))
            .andExpect(jsonPath("$[0].matchLog").value("matchlogs"))
            .andExpect(jsonPath("$[0].team2GlickoChange").value(100.0))
            .andExpect(jsonPath("$[0].team1GlickoChange").value(-100.0));

        verify(gameMatchLogService).getAllGameMatchLogs();
    }

    @Test
    void getGameMatchLogById() throws Exception {
        Long id = 2L;
        when(gameMatchLogService.getGameMatchLogById(id)).thenReturn(Optional.of(gameMatchLog));

        mockMvc.perform(get("/api/v1/game-match-log/id").param("id", id.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(2L))
            .andExpect(jsonPath("$.matchId").value(1L))
            .andExpect(jsonPath("$.matchLog").value("matchlogs"))
            .andExpect(jsonPath("$.team2GlickoChange").value(100.0))
            .andExpect(jsonPath("$.team1GlickoChange").value(-100.0));

        verify(gameMatchLogService).getGameMatchLogById(id);
    }

    @Test
    void getGameMatchLogsIds() throws Exception {
        List<Long> ids = List.of(2L, 3L);
        when(gameMatchLogService.getGameMatchLogIds()).thenReturn(ids);

        mockMvc.perform(get("/api/v1/game-match-log/ids"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value(2L))
            .andExpect(jsonPath("$[1]").value(3L));

        verify(gameMatchLogService).getGameMatchLogIds();
    }
}
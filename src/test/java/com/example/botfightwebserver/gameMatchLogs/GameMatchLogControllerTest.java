package com.example.botfightwebserver.gameMatchLogs;

import com.example.botfightwebserver.player.PlayerController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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

        mockMvc.perform(get("/api/v1/game-match-log"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(2L))
            .andExpect(jsonPath("$[0].matchId").value(1L))
            .andExpect(jsonPath("$[0].matchLog").value("matchlogs"))
            .andExpect(jsonPath("$[0].team2GlickoChange").value(100.0))
            .andExpect(jsonPath("$[0].team1GlickoChange").value(-100.0));

        verify(gameMatchLogService).getAllGameMatchLogs();
    }
}
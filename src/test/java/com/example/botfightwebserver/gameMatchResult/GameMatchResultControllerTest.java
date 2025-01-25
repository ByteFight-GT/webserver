package com.example.botfightwebserver.gameMatchResult;

import com.example.botfightwebserver.SecurityTestConfig;
import com.example.botfightwebserver.gameMatch.GameMatchController;
import com.example.botfightwebserver.gameMatch.MATCH_STATUS;
import com.example.botfightwebserver.gameMatch.TestJwtFilter;
import com.example.botfightwebserver.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = GameMatchResultController.class, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
})
@Import({SecurityTestConfig.class, TestJwtFilter.class})
@WithMockUser(roles = "ADMIN")
class GameMatchResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GameMatchResultHandler gameMatchResultHandler;

    private GameMatchResult sampleResult;
    private List<GameMatchResult> sampleResults;

    @BeforeEach
    void setUp() {
        sampleResult = new GameMatchResult(1L, MATCH_STATUS.TEAM_ONE_WIN, "Sample match log");
        sampleResults = Arrays.asList(
            new GameMatchResult(1L, MATCH_STATUS.TEAM_ONE_WIN, "Log 1"),
            new GameMatchResult(2L, MATCH_STATUS.TEAM_TWO_WIN, "Log 2")
        );
    }

    @Test
    void handleMatchResults_Success() throws Exception {
        doNothing().when(gameMatchResultHandler).handleGameMatchResult(any(GameMatchResult.class));

        mockMvc.perform(post("/api/v1/game-match-result/handle/results").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleResult)))
            .andExpect(status().isNoContent());

        verify(gameMatchResultHandler).handleGameMatchResult(any(GameMatchResult.class));
    }

    @Test
    void handleMatchResults_Error() throws Exception {
        String errorMessage = "Match not found";
        doThrow(new IllegalArgumentException(errorMessage))
            .when(gameMatchResultHandler).handleGameMatchResult(any(GameMatchResult.class));

        mockMvc.perform(post("/api/v1/game-match-result/handle/results").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleResult)))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(errorMessage));

        verify(gameMatchResultHandler).handleGameMatchResult(any(GameMatchResult.class));
    }

    @Test
    void submitResults_Success() throws Exception {
        doNothing().when(gameMatchResultHandler).submitGameMatchResults(any(GameMatchResult.class));

        mockMvc.perform(post("/api/v1/game-match-result/submit/results").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleResult)))
            .andExpect(status().isAccepted());

        verify(gameMatchResultHandler).submitGameMatchResults(any(GameMatchResult.class));
    }

    @Test
    void submitResults_Error() throws Exception {
        String errorMessage = "Failed to submit";
        doThrow(new RuntimeException(errorMessage))
            .when(gameMatchResultHandler).submitGameMatchResults(any(GameMatchResult.class));

        mockMvc.perform(post("/api/v1/game-match-result/submit/results").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleResult)))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(errorMessage));

        verify(gameMatchResultHandler).submitGameMatchResults(any(GameMatchResult.class));
    }

    @Test
    void removeAllQueuedResults_Success() throws Exception {
        when(gameMatchResultHandler.deleteQueuedMatches()).thenReturn(sampleResults);

        mockMvc.perform(post("/api/v1/game-match-result/queue/remove_all").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].matchId").value(sampleResults.get(0).matchId()))
            .andExpect(jsonPath("$[0].status").value(sampleResults.get(0).status().toString()))
            .andExpect(jsonPath("$[1].matchId").value(sampleResults.get(1).matchId()))
            .andExpect(jsonPath("$[1].status").value(sampleResults.get(1).status().toString()));

        verify(gameMatchResultHandler).deleteQueuedMatches();
    }

    @Test
    void removeAllQueuedResults_Error() throws Exception {
        String errorMessage = "Failed to delete queue";
        when(gameMatchResultHandler.deleteQueuedMatches())
            .thenThrow(new RuntimeException(errorMessage));

        mockMvc.perform(post("/api/v1/game-match-result/queue/remove_all").with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(errorMessage));

        verify(gameMatchResultHandler).deleteQueuedMatches();
    }

}
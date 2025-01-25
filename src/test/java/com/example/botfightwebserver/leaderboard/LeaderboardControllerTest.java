package com.example.botfightwebserver.leaderboard;

import com.example.botfightwebserver.SecurityTestConfig;
import com.example.botfightwebserver.gameMatch.TestJwtFilter;
import com.example.botfightwebserver.gameMatchLogs.GameMatchLogController;
import com.example.botfightwebserver.security.JwtAuthFilter;
import com.example.botfightwebserver.team.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = LeaderboardController.class, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
})
@Import({SecurityTestConfig.class, TestJwtFilter.class})
@WithMockUser()
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamService teamService;

    private static final LeaderboardDTO TEST_LEADERBOARD_ENTRY = LeaderboardDTO.builder()
        .teamId(1L)
        .rank(1)
        .teamName("Test Team")
        .glicko(1500.0)
        .createdAt(LocalDateTime.of(2024, 1, 1, 12, 0))
        .members(List.of("Player1", "Player2"))
        .quote("Test Quote")
        .build();

    @Test
    void shouldGetLeaderboard() throws Exception {
        List<LeaderboardDTO> leaderboard = List.of(TEST_LEADERBOARD_ENTRY);
        when(teamService.getLeaderboard()).thenReturn(leaderboard);

        mockMvc.perform(get("/api/v1/leaderboard/public/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].teamId").value(1))
            .andExpect(jsonPath("$[0].rank").value(1))
            .andExpect(jsonPath("$[0].teamName").value("Test Team"))
            .andExpect(jsonPath("$[0].glicko").value(1500.0))
            .andExpect(jsonPath("$[0].members[0]").value("Player1"))
            .andExpect(jsonPath("$[0].members[1]").value("Player2"))
            .andExpect(jsonPath("$[0].quote").value("Test Quote"));

        verify(teamService).getLeaderboard();
    }
}
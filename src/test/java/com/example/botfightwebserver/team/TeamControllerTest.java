package com.example.botfightwebserver.team;

import com.example.botfightwebserver.SecurityTestConfig;
import com.example.botfightwebserver.gameMatch.GameMatchController;
import com.example.botfightwebserver.gameMatch.TestJwtFilter;
import com.example.botfightwebserver.glicko.GlickoHistoryDTO;
import com.example.botfightwebserver.player.Player;
import com.example.botfightwebserver.player.PlayerService;
import com.example.botfightwebserver.security.JwtAuthFilter;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TeamController.class, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
})
@Import({SecurityTestConfig.class, TestJwtFilter.class})
@WithMockUser()
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtFilter jwtAuthFilter;

    @MockBean
    private TeamService teamService;

    @MockBean
    private PlayerService playerService;

    @Test
    void testGetTeams() throws Exception {
        Team team1 = Team.builder().id(1L).name("tyler").build();
        Team team2 = Team.builder().id(2L).name("ben").build();
        List<Team> teams = List.of(team1, team2);

        when(teamService.getTeams()).thenReturn(teams);

        mockMvc.perform(get("/api/v1/team/teams"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].name").value("tyler"))
            .andExpect(jsonPath("$[1].id").value(2L))
            .andExpect(jsonPath("$[1].name").value("ben"));

        verify(teamService).getTeams();
    }

    @Test
    void testGetTeamsEmpty() throws Exception {
        List<Team> emptyTeams = List.of();
        when(teamService.getTeams()).thenReturn(emptyTeams);

        mockMvc.perform(get("/api/v1/team/teams"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testCreateTeam() throws Exception {
        Team newTeam =
            Team.builder()
                .id(1L)
                .name("tyler")
                .glicko(1500.0)
                .matchesPlayed(0)
                .numberLosses(0)
                .numberWins(0)
                .numberDraws(0).build();

        when(teamService.createTeam("tyler")).thenReturn(newTeam);
        when(playerService.setPlayerTeam(any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/v1/team").with(csrf())
                .param("name", "tyler")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("tyler"))
            .andExpect(jsonPath("$.matchesPlayed").value(0))
            .andExpect(jsonPath("$.numberLosses").value(0))
            .andExpect(jsonPath("$.numberWins").value(0))
            .andExpect(jsonPath("$.numberDraws").value(0));
    }

    @Test
    void testCreateTeamNameExists() throws Exception {
        String name = "tyler";
        when(teamService.createTeam("tyler")).thenThrow(
            new IllegalArgumentException("Team with name " + name + " already exists"));

        Team newTeam =
            Team.builder()
                .id(1L)
                .name("tyler")
                .glicko(1200.0)
                .matchesPlayed(0)
                .numberLosses(0)
                .numberWins(0)
                .numberDraws(0).build();

        mockMvc.perform(post("/api/v1/team").with(csrf())
            .param("name", "tyler")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Team with name " + name + " already exists")));
    }

    @Test
    void testSetQuote() throws Exception {
        UUID authId = UUID.randomUUID();
        Player player = Player.builder().authId(authId).teamId(1L).build();
        Long teamId = 1L;
        String quote = "We are the champions!";
        when(playerService.getPlayer((UUID) any())).thenReturn(player);
        mockMvc.perform(post("/api/v1/team/quote").with(csrf())
                .param("teamId", teamId.toString())
                .param("quote", quote)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(quote));
    }

    @Test
    void testSetQuoteTeamNotFound() throws Exception {
        UUID authId = UUID.randomUUID();
        Player player = Player.builder().authId(authId).teamId(999L).build();
        Long teamId = 999L;
        String quote = "We are the champions!";

        when(playerService.getPlayer((UUID) any())).thenReturn(player);

        doThrow(new IllegalArgumentException("Team not found with id: " + teamId))
            .when(teamService).setQuote(teamId, quote);

        mockMvc.perform(post("/api/v1/team/quote").with(csrf())
                .param("teamId", teamId.toString())
                .param("quote", quote)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Team not found with id: " + teamId)));
    }
}
package com.example.botfightwebserver.team;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamService teamService;

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
                .glicko(1200.0)
                .matchesPlayed(0)
                .numberLosses(0)
                .numberWins(0)
                .numberDraws(0).build();

        when(teamService.createTeam("tyler")).thenReturn(newTeam);

        mockMvc.perform(post("/api/v1/team")
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

        mockMvc.perform(post("/api/v1/team")
            .param("name", "tyler")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Team with name " + name + " already exists")));
    }

    @Test
    void testSetQuote() throws Exception {
        Long teamId = 1L;
        String quote = "We are the champions!";

        mockMvc.perform(post("/api/v1/team/quote")
                .param("teamId", teamId.toString())
                .param("quote", quote)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(quote));
    }

    @Test
    void testSetQuoteTeamNotFound() throws Exception {
        Long teamId = 999L;
        String quote = "We are the champions!";

        doThrow(new IllegalArgumentException("Team not found with id: " + teamId))
            .when(teamService).setQuote(teamId, quote);

        mockMvc.perform(post("/api/v1/team/quote")
                .param("teamId", teamId.toString())
                .param("quote", quote)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Team not found with id: " + teamId)));
    }
}
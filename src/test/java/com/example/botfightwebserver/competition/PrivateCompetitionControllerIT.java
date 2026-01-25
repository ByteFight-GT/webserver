package com.example.botfightwebserver.competition;

import com.example.botfightwebserver.FullStackIntegrationTestBase;
import com.example.botfightwebserver.TestDataFactory;
import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.competition.domain.Competition;
import com.example.botfightwebserver.competition.domain.dto.JoinTeamDto;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.domain.dto.SelfTeamDto;
import com.example.botfightwebserver.team.domain.dto.TeamSettingsDto;
import com.example.botfightwebserver.team.application.TeamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PrivateCompetitionControllerIT extends FullStackIntegrationTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private TeamService teamService;


    @Test
    void createCompetitionTeamHappyPath() throws Exception {
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        Competition competition = testDataFactory.createCompetition();

        TeamSettingsDto dto = TeamSettingsDto.builder()
                .name("Awesome Team")
                .quote("We are so cool")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/competition/{slug}/teams", competition.getSlug())
                .with(user(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        SelfTeamDto responseDto = objectMapper.readValue(result.getResponse().getContentAsString(), SelfTeamDto.class);

        assertThat(responseDto.getName()).isEqualTo("Awesome Team");
        assertThat(responseDto.getQuote()).isEqualTo("We are so cool");
    }

    @Test
    void createCompetitionTeamCompetitionNotFound() throws Exception {
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();

        TeamSettingsDto dto1 = TeamSettingsDto.builder()
                .name("Team Name")
                .build();

        mockMvc.perform(post("/api/v1/competition/{slug}/teams", "nonexistent-competition")
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    void createCompetitionTeamDuplicateTeamName() throws Exception {
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        Competition competition = testDataFactory.createCompetition();

        TeamSettingsDto dto1 = TeamSettingsDto.builder()
                .name("Team Name")
                .build();

        mockMvc.perform(post("/api/v1/competition/{slug}/teams", competition.getSlug())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isOk())
                .andReturn();

        // Team names cannot be identical up to differences in capitalization
        TeamSettingsDto dto2 = TeamSettingsDto.builder()
                .name("TeAm nAmE")
                .build();

        mockMvc.perform(post("/api/v1/competition/{slug}/teams", competition.getSlug())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createCompetitionTeamPlayerAlreadyInCompetition() throws Exception {
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        Competition competition = testDataFactory.createCompetition();

        TeamSettingsDto dto1 = TeamSettingsDto.builder()
                .name("Team 1")
                .build();

        TeamSettingsDto dto2 = TeamSettingsDto.builder()
                .name("Team 2")
                .build();

        // Player joins Team 1 by creating Team 1
        mockMvc.perform(post("/api/v1/competition/{slug}/teams", competition.getSlug())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isOk());

        // Player should not be able to create Team 2 since they are already in a team
        mockMvc.perform(post("/api/v1/competition/{slug}/teams", competition.getSlug())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createCompetitionTeamCompetitionIsInactive() throws Exception {
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        Competition competition = testDataFactory.createCompetition("test", "test", false, 2);

        TeamSettingsDto dto1 = TeamSettingsDto.builder()
                .name("Team 1")
                .build();

        mockMvc.perform(post("/api/v1/competition/{slug}/teams", competition.getSlug())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void joinCompetitionTeamHappyPath() throws Exception {
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        Competition competition = testDataFactory.createCompetition();
        Team team = testDataFactory.createTeam(competition);

        JoinTeamDto dto = JoinTeamDto.builder()
                .joinCode(team.getJoinCode())
                .build();

        mockMvc.perform(post("/api/v1/competition/{slug}/teams/join", competition.getSlug())
                .with(user(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void joinCompetitionTeamTeamNotFound() throws Exception {
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        Competition competition = testDataFactory.createCompetition();
        testDataFactory.createTeam(competition);

        JoinTeamDto dto = JoinTeamDto.builder()
                .joinCode("blah")
                .build();

        mockMvc.perform(post("/api/v1/competition/{slug}/teams/join", competition.getSlug())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void joinCompetitionTeamPlayerAlreadyInTeam() throws Exception {
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        Competition competition = testDataFactory.createCompetition();
        Team team1 = testDataFactory.createTeam(competition);
        Team team2 = testDataFactory.createTeam(competition);

        JoinTeamDto dto1 = JoinTeamDto.builder()
                .joinCode(team1.getJoinCode())
                .build();

        JoinTeamDto dto2 = JoinTeamDto.builder()
                .joinCode(team2.getJoinCode())
                .build();

        mockMvc.perform(post("/api/v1/competition/{slug}/teams/join", competition.getSlug())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/competition/{slug}/teams/join", competition.getSlug())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void joinCompetitionTeamTeamIsFull() throws Exception {
        Player player1 = testDataFactory.createUserWithPlayer();
        Player player2 = testDataFactory.createUserWithPlayer();

        User user1 = player1.getUser();
        User user2 = player2.getUser();

        Competition competition = testDataFactory.createCompetition("test", "test", true, 1);
        Team team = testDataFactory.createTeam(competition);

        JoinTeamDto dto = JoinTeamDto.builder()
                .joinCode(team.getJoinCode())
                .build();

        mockMvc.perform(post("/api/v1/competition/{slug}/teams/join", competition.getSlug())
                        .with(user(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/competition/{slug}/teams/join", competition.getSlug())
                        .with(user(user2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void joinCompetitionTeamCompetitionIsInactive() throws Exception {
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        Competition competition = testDataFactory.createCompetition("test", "test", false, 2);
        Team team = testDataFactory.createTeam(competition);

        JoinTeamDto dto = JoinTeamDto.builder()
                .joinCode(team.getJoinCode())
                .build();

        mockMvc.perform(post("/api/v1/competition/{slug}/teams/join", competition.getSlug())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Transactional
    void leaveCompetitionTeamHappyPath() throws Exception {
        Player player = testDataFactory.createUserWithPlayer();
        Player otherPlayer = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        Competition competition = testDataFactory.createCompetition();
        Team team = testDataFactory.createTeam(competition);

        teamService.joinTeam(player, team);
        teamService.joinTeam(otherPlayer, team);

        mockMvc.perform(post("/api/v1/competition/{slug}/teams/leave", competition.getSlug())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        List<Player> remainingMembers = teamService.getPlayersForTeam(team);
        assertThat(remainingMembers).hasSize(1);
        assertThat(remainingMembers.get(0).getUser().getUuid()).isEqualTo(otherPlayer.getUser().getUuid());
    }

    @Test
    void leaveCompetitionTeamAllowsJoinAnotherTeam() throws Exception {
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        Competition competition = testDataFactory.createCompetition();
        Team team1 = testDataFactory.createTeam(competition);
        Team team2 = testDataFactory.createTeam(competition);

        JoinTeamDto team1JoinDto = JoinTeamDto.builder()
                .joinCode(team1.getJoinCode())
                .build();

        JoinTeamDto team2JoinDto = JoinTeamDto.builder()
                .joinCode(team2.getJoinCode())
                .build();

        mockMvc.perform(post("/api/v1/competition/{slug}/teams/join", competition.getSlug())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(team1JoinDto)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/competition/{slug}/teams/leave", competition.getSlug())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/competition/{slug}/teams/join", competition.getSlug())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(team2JoinDto)))
                .andExpect(status().isOk());

        assertThat(teamService.countPlayersForTeam(team1)).isZero();
        assertThat(teamService.countPlayersForTeam(team2)).isEqualTo(1);
    }

    @Test
    void leaveCompetitionTeamDeletesTeamWhenNoMembersRemain() throws Exception {
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        Competition competition = testDataFactory.createCompetition();
        Team team = testDataFactory.createTeam(competition);

        teamService.joinTeam(player, team);

        mockMvc.perform(post("/api/v1/competition/{slug}/teams/leave", competition.getSlug())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Team updatedTeam = teamService.getTeamByCompetitionAndUuid(competition, team.getUuid()).orElseThrow();
        assertThat(updatedTeam.isDeleted()).isTrue();
    }
}

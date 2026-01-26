package org.bytefight.webserver.team;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.permissions.infra.PermissionsRespository;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.dto.TeamSettingsDto;
import org.bytefight.webserver.team.infra.TeamRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PrivateTeamControllerIT extends FullStackIntegrationTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PermissionsRespository permissionsRespository;

    @Test
    void editTeamUpdatesTeamSettings() throws Exception {
        Competition competition = testDataFactory.createCompetition();
        Team team = testDataFactory.createTeam(competition);
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        teamService.joinTeam(player, team);

        TeamSettingsDto dto = TeamSettingsDto.builder()
                .name("Updated Name")
                .quote("New quote")
                .displayMembers(true)
                .build();

        mockMvc.perform(post("/api/v1/team/{uuid}", team.getUuid())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.quote").value("New quote"))
                .andExpect(jsonPath("$.displayMembers").value(true));

        Team updated = teamRepository.findByUuid(team.getUuid()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getQuote()).isEqualTo("New quote");
        assertThat(updated.isDisplayMembers()).isTrue();
    }

    @Test
    void editTeamRejectsNonMember() throws Exception {
        Competition competition = testDataFactory.createCompetition();
        Team team = testDataFactory.createTeam(competition);
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();

        TeamSettingsDto dto = TeamSettingsDto.builder()
                .name("Updated Name")
                .displayMembers(true)
                .build();

        mockMvc.perform(post("/api/v1/team/{uuid}", team.getUuid())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void editTeamRejectsDuplicateName() throws Exception {
        Competition competition = testDataFactory.createCompetition();
        Team team = testDataFactory.createTeam(competition);
        Team otherTeam = testDataFactory.createTeam(competition);
        otherTeam.setName("Taken Name");
        teamRepository.save(otherTeam);

        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        teamService.joinTeam(player, team);

        TeamSettingsDto dto = TeamSettingsDto.builder()
                .name("taken name")
                .displayMembers(true)
                .build();

        mockMvc.perform(post("/api/v1/team/{uuid}", team.getUuid())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void editTeamNotFound() throws Exception {
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();

        TeamSettingsDto dto = TeamSettingsDto.builder()
                .name("Updated Name")
                .displayMembers(true)
                .build();

        mockMvc.perform(post("/api/v1/team/{uuid}", UUID.randomUUID())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }
}

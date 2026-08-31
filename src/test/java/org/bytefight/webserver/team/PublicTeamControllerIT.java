package org.bytefight.webserver.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.player.domain.dto.PublicPlayerDto;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.dto.PublicTeamDto;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@Transactional
class PublicTeamControllerIT extends FullStackIntegrationTestBase {
  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private TestDataFactory testDataFactory;

  @Autowired private TeamService teamService;

  @Autowired private TeamRepository teamRepository;

  @Test
  void getPublicTeamByUuidReturnsTeamWithoutMembersWhenDisplayMembersFalse() throws Exception {
    Competition competition = testDataFactory.createCompetition();
    Team team = testDataFactory.createTeam(competition);
    team.setDisplayMembers(false);
    teamRepository.save(team);

    Player player1 = testDataFactory.createUserWithPlayer();
    Player player2 = testDataFactory.createUserWithPlayer();

    teamService.joinTeam(player1, team);
    teamService.joinTeam(player2, team);

    MvcResult result =
        mockMvc
            .perform(get("/api/v1/public/team/{uuid}", team.getUuid()))
            .andExpect(status().isOk())
            .andReturn();

    PublicTeamDto dto =
        objectMapper.readValue(result.getResponse().getContentAsString(), PublicTeamDto.class);

    assertThat(dto.getDisplayMembers()).isFalse();
    assertThat(dto.getMembers()).isNull();
  }

  @Test
  void getPublicTeamByUuidReturnsTeamWithMembersWhenDisplayMembersTrue() throws Exception {
    Competition competition = testDataFactory.createCompetition();
    Team team = testDataFactory.createTeam(competition);
    team.setDisplayMembers(true);
    teamRepository.save(team);

    Player player1 = testDataFactory.createUserWithPlayer();
    Player player2 = testDataFactory.createUserWithPlayer();

    teamService.joinTeam(player1, team);
    teamService.joinTeam(player2, team);

    MvcResult result =
        mockMvc
            .perform(get("/api/v1/public/team/{uuid}", team.getUuid()))
            .andExpect(status().isOk())
            .andReturn();

    PublicTeamDto dto =
        objectMapper.readValue(result.getResponse().getContentAsString(), PublicTeamDto.class);

    List<String> usernames = dto.getMembers().stream().map(PublicPlayerDto::getUsername).toList();
    assertThat(dto.getDisplayMembers()).isTrue();
    assertThat(usernames).contains(player1.getUsername(), player2.getUsername());
  }

  @Test
  void getPublicTeamByUuidNotFound() throws Exception {
    mockMvc
        .perform(get("/api/v1/public/team/{uuid}", UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }

  @Test
  void getPublicTeamByUuidDeletedTeamNotFound() throws Exception {
    Competition competition = testDataFactory.createCompetition();
    Team team = testDataFactory.createTeam(competition);
    team.softDelete();
    teamRepository.save(team);

    mockMvc
        .perform(get("/api/v1/public/team/{uuid}", team.getUuid()))
        .andExpect(status().isNotFound());
  }
}

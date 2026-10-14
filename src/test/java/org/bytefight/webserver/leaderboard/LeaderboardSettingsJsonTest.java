package org.bytefight.webserver.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.domain.dto.AdminCompetitionDto;
import org.bytefight.webserver.competition.domain.dto.AdminUpdateCompetitionDto;
import org.bytefight.webserver.competition.domain.dto.CompetitionDto;
import org.bytefight.webserver.leaderboard.domain.AdminUpdateLeaderboardSettingsDto;
import org.bytefight.webserver.leaderboard.domain.LeaderboardSettingsDto;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

class LeaderboardSettingsJsonTest {
  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .registerModule(new ParameterNamesModule());

  @Test
  void publicAndAdminPayloadsShareLeaderboardSettingNames() throws Exception {
    Competition competition = new Competition();
    competition.setSlug("bf2026");
    competition.setName("ByteFight");
    competition.setLeaderboardPagination(true);
    competition.setLeaderboardPageSize(10);

    JsonNode publicJson =
        objectMapper.readTree(objectMapper.writeValueAsString(CompetitionDto.from(competition)));
    JsonNode adminJson =
        objectMapper.readTree(
            objectMapper.writeValueAsString(AdminCompetitionDto.from(competition)));
    JsonNode settingsJson =
        objectMapper.readTree(
            objectMapper.writeValueAsString(LeaderboardSettingsDto.from(competition)));

    assertThat(publicJson.get("leaderboardPagination").asBoolean()).isTrue();
    assertThat(publicJson.get("leaderboardPageSize").asInt()).isEqualTo(10);
    assertThat(adminJson.get("leaderboardPagination").asBoolean()).isTrue();
    assertThat(adminJson.get("leaderboardPageSize").asInt()).isEqualTo(10);
    assertThat(settingsJson.get("leaderboardPagination").asBoolean()).isTrue();
    assertThat(settingsJson.get("leaderboardPageSize").asInt()).isEqualTo(10);
  }

  @Test
  void partialUpdatesLeaveOmittedSettingsNull() throws Exception {
    AdminUpdateLeaderboardSettingsDto leaderboardUpdate =
        objectMapper.readValue(
            "{\"leaderboardPagination\":true}", AdminUpdateLeaderboardSettingsDto.class);
    AdminUpdateCompetitionDto competitionUpdate =
        objectMapper.readValue("{\"leaderboardPageSize\":40}", AdminUpdateCompetitionDto.class);

    assertThat(leaderboardUpdate.getLeaderboardPagination()).isTrue();
    assertThat(leaderboardUpdate.getLeaderboardPageSize()).isNull();
    assertThat(competitionUpdate.getLeaderboardPagination()).isNull();
    assertThat(competitionUpdate.getLeaderboardPageSize()).isEqualTo(40);
    assertThat(competitionUpdate.getName()).isNull();
  }
}

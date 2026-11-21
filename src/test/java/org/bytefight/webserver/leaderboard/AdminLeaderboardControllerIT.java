package org.bytefight.webserver.leaderboard;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@Transactional
class AdminLeaderboardControllerIT extends FullStackIntegrationTestBase {
  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private TestDataFactory testDataFactory;

  @Test
  void defaultsAreSinglePageWithTwentyFiveRows() throws Exception {
    Competition competition =
        testDataFactory.createCompetition("lb-default", "LB Default", true, 2);

    mockMvc
        .perform(adminJson(get("/api/v1/admin/leaderboard/{id}", competition.getId()), null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.competitionId").value(competition.getId()))
        .andExpect(jsonPath("$.leaderboardPagination").value(false))
        .andExpect(
            jsonPath("$.leaderboardPageSize").value(Competition.DEFAULT_LEADERBOARD_PAGE_SIZE));

    mockMvc
        .perform(get("/api/v1/public/competition/{slug}", competition.getSlug()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leaderboardPagination").value(false))
        .andExpect(
            jsonPath("$.leaderboardPageSize").value(Competition.DEFAULT_LEADERBOARD_PAGE_SIZE));
  }

  @Test
  void adminEndpointAndCompetitionRulesShareTheSameSettings() throws Exception {
    Competition competition = testDataFactory.createCompetition("lb-rules", "LB Rules", true, 2);

    mockMvc
        .perform(
            adminJson(
                patch("/api/v1/admin/leaderboard/{id}", competition.getId()),
                Map.of("leaderboardPagination", true)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leaderboardPagination").value(true))
        .andExpect(
            jsonPath("$.leaderboardPageSize").value(Competition.DEFAULT_LEADERBOARD_PAGE_SIZE));

    mockMvc
        .perform(
            adminJson(
                patch("/api/v1/admin/competition/{id}", competition.getId()),
                Map.of("leaderboardPageSize", 10)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leaderboardPagination").value(true))
        .andExpect(jsonPath("$.leaderboardPageSize").value(10));

    mockMvc
        .perform(get("/api/v1/public/competition/{slug}", competition.getSlug()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leaderboardPagination").value(true))
        .andExpect(jsonPath("$.leaderboardPageSize").value(10));
  }

  @Test
  void newCompetitionStartsAsASinglePageAndEmptyUpdatesDoNotChangeIt() throws Exception {
    mockMvc
        .perform(
            adminJson(
                post("/api/v1/admin/competition"),
                Map.of("slug", "created_lb", "name", "Created LB")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.leaderboardPagination").value(false))
        .andExpect(
            jsonPath("$.leaderboardPageSize").value(Competition.DEFAULT_LEADERBOARD_PAGE_SIZE));

    Competition competition = testDataFactory.createCompetition("lb-noop", "LB Noop", true, 2);
    competition.setLeaderboardPagination(true);
    competition.setLeaderboardPageSize(12);

    mockMvc
        .perform(adminJson(patch("/api/v1/admin/leaderboard/{id}", competition.getId()), Map.of()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leaderboardPagination").value(true))
        .andExpect(jsonPath("$.leaderboardPageSize").value(12));

    mockMvc
        .perform(get("/api/v1/admin/leaderboard/{id}", 999_999L).with(user("admin").roles("ADMIN")))
        .andExpect(status().isNotFound());
  }

  @Test
  void pageSizeAcceptsTheBoundaries() throws Exception {
    Competition competition = testDataFactory.createCompetition("lb-edges", "LB Edges", true, 2);

    mockMvc
        .perform(
            adminJson(
                patch("/api/v1/admin/leaderboard/{id}", competition.getId()),
                Map.of("leaderboardPageSize", 1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leaderboardPageSize").value(1));

    mockMvc
        .perform(
            adminJson(
                patch("/api/v1/admin/competition/{id}", competition.getId()),
                Map.of("leaderboardPageSize", Competition.MAX_LEADERBOARD_PAGE_SIZE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leaderboardPageSize").value(Competition.MAX_LEADERBOARD_PAGE_SIZE));
  }

  @Test
  void rejectsPageSizeOutsideTheAllowedRange() throws Exception {
    Competition competition = testDataFactory.createCompetition("lb-bounds", "LB Bounds", true, 2);

    mockMvc
        .perform(
            adminJson(
                patch("/api/v1/admin/leaderboard/{id}", competition.getId()),
                Map.of("leaderboardPageSize", 0)))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            adminJson(
                patch("/api/v1/admin/competition/{id}", competition.getId()),
                Map.of("leaderboardPageSize", Competition.MAX_LEADERBOARD_PAGE_SIZE + 1)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void settingsRequireAnAdmin() throws Exception {
    Competition competition = testDataFactory.createCompetition("lb-auth", "LB Auth", true, 2);

    mockMvc
        .perform(get("/api/v1/admin/leaderboard/{id}", competition.getId()))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            patch("/api/v1/admin/leaderboard/{id}", competition.getId())
                .with(user("player").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"leaderboardPagination\":true}"))
        .andExpect(status().isForbidden());
  }

  private MockHttpServletRequestBuilder adminJson(
      MockHttpServletRequestBuilder request, Map<String, Object> body) throws Exception {
    request.with(user("admin").roles("ADMIN"));
    if (body != null) {
      request
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(body));
    }
    return request;
  }
}

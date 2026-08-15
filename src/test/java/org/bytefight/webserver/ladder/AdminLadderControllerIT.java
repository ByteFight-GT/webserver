package org.bytefight.webserver.ladder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.Map;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Covers {@code maxQueuedPerTeam} on the admin ladder endpoints (webserver#159). Before the fix a
 * freshly created ladder came back with 0, which made every user-created match on it 429.
 */
@Transactional
class AdminLadderControllerIT extends FullStackIntegrationTestBase {
  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private TestDataFactory testDataFactory;

  @Test
  void createdLadderDefaultsMaxQueuedPerTeam() throws Exception {
    Competition competition = testDataFactory.createCompetition();

    mockMvc
        .perform(adminJson(post("/api/v1/admin/ladder"), createBody(competition, "scrim", null)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.maxQueuedPerTeam").value(Ladder.DEFAULT_MAX_QUEUED_PER_TEAM));
  }

  @Test
  void createdLadderHonoursExplicitMaxQueuedPerTeam() throws Exception {
    Competition competition = testDataFactory.createCompetition();

    mockMvc
        .perform(adminJson(post("/api/v1/admin/ladder"), createBody(competition, "scrim", 3)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.maxQueuedPerTeam").value(3));
  }

  @Test
  void createRejectsNonPositiveMaxQueuedPerTeam() throws Exception {
    Competition competition = testDataFactory.createCompetition();

    mockMvc
        .perform(adminJson(post("/api/v1/admin/ladder"), createBody(competition, "scrim", 0)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateChangesMaxQueuedPerTeam() throws Exception {
    Competition competition = testDataFactory.createCompetition();
    Ladder ladder = testDataFactory.createLadder(competition, "scrim");

    mockMvc
        .perform(
            adminJson(
                patch("/api/v1/admin/ladder/{id}", ladder.getId()), Map.of("maxQueuedPerTeam", 5)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.maxQueuedPerTeam").value(5));
  }

  private MockHttpServletRequestBuilder adminJson(
      MockHttpServletRequestBuilder request, Map<String, Object> body) throws Exception {
    return request
        .with(user("admin").roles("ADMIN"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body));
  }

  /** Built as a map rather than the DTO so the test does not depend on its constructor order. */
  private Map<String, Object> createBody(
      Competition competition, String ladderSlug, Integer maxQueuedPerTeam) {
    Map<String, Object> body = new HashMap<>();
    body.put("competitionId", competition.getId());
    body.put("ladder", ladderSlug);
    if (maxQueuedPerTeam != null) {
      body.put("maxQueuedPerTeam", maxQueuedPerTeam);
    }
    return body;
  }
}

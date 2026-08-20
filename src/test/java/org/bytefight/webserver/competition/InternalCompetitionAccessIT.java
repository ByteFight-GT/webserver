package org.bytefight.webserver.competition;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An internal competition, and everything reachable through it, must be invisible to anyone who is
 * not an admin — whether they come in by competition slug or indirectly by team or match id.
 */
@Transactional
class InternalCompetitionAccessIT extends FullStackIntegrationTestBase {
  @Autowired private MockMvc mockMvc;

  @Autowired private TestDataFactory testDataFactory;

  @Autowired private ObjectMapper objectMapper;

  private Competition internal;
  private Team internalTeam;
  private User admin;
  private User nonAdmin;
  private User serviceAccount;

  @BeforeEach
  void setUp() {
    internal = testDataFactory.createInternalCompetition("hidden-comp");
    internalTeam = testDataFactory.createTeam(internal);
    testDataFactory.createLadder(internal, "main");
    admin = testDataFactory.createUser(null, true);
    nonAdmin = testDataFactory.createUser(null, false);
    serviceAccount = testDataFactory.createServiceAccount();
  }

  @Test
  void competitionBySlugIsHiddenFromNonAdmins() throws Exception {
    mockMvc
        .perform(get("/api/v1/public/competition/{slug}", internal.getSlug()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/public/competition/{slug}", internal.getSlug()).with(user(nonAdmin)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/public/competition/{slug}", internal.getSlug()).with(user(admin)))
        .andExpect(status().isOk());
  }

  @Test
  void teamInInternalCompetitionIsHiddenFromNonAdmins() throws Exception {
    mockMvc
        .perform(get("/api/v1/public/team/{uuid}", internalTeam.getUuid()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/public/team/{uuid}", internalTeam.getUuid()).with(user(admin)))
        .andExpect(status().isOk());
  }

  @Test
  void teamStatsForInternalCompetitionAreHiddenFromNonAdmins() throws Exception {
    mockMvc
        .perform(get("/api/v1/public/team-stats/{uuid}", internalTeam.getUuid()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get("/api/v1/public/team-stats/{uuid}/{ladder}", internalTeam.getUuid(), "main")
                .with(user(nonAdmin)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get("/api/v1/public/team-stats/{uuid}/{ladder}", internalTeam.getUuid(), "main")
                .with(user(admin)))
        .andExpect(status().isOk());
  }

  @Test
  void laddersForInternalCompetitionAreHiddenFromNonAdmins() throws Exception {
    mockMvc
        .perform(get("/api/v1/public/ladder/{slug}", internal.getSlug()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/public/ladder/{slug}", internal.getSlug()).with(user(admin)))
        .andExpect(status().isOk());
  }

  @Test
  void matchQueueForInternalCompetitionIsHiddenFromNonAdmins() throws Exception {
    mockMvc
        .perform(get("/api/v1/public/game-match/queue/{slug}", internal.getSlug()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/public/game-match").param("competitionSlug", internal.getSlug()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get("/api/v1/public/game-match")
                .param("competitionSlug", internal.getSlug())
                .with(user(admin)))
        .andExpect(status().isOk());
  }

  @Test
  void joiningAnInternalCompetitionIsRejectedForNonAdmins() throws Exception {
    Player player = testDataFactory.createUserWithPlayer();

    mockMvc
        .perform(
            post("/api/v1/competition/{slug}/teams", internal.getSlug())
                .with(user(player.getUser()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("name", "Sneaky Team", "displayMembers", false))))
        .andExpect(status().isNotFound());
  }

  @Test
  void serviceAccountsReachInternalCompetitionsLikeAdmins() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/public/competition/{slug}", internal.getSlug()).with(user(serviceAccount)))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            get("/api/v1/public/team/{uuid}", internalTeam.getUuid()).with(user(serviceAccount)))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/v1/public/ladder/{slug}", internal.getSlug()).with(user(serviceAccount)))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            get("/api/v1/public/game-match")
                .param("competitionSlug", internal.getSlug())
                .with(user(serviceAccount)))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/v1/public/competition/").with(user(serviceAccount)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].slug").value(org.hamcrest.Matchers.hasItem(internal.getSlug())));
  }

  @Test
  void nonInternalCompetitionsStayReachable() throws Exception {
    Competition open = testDataFactory.createCompetition("open-comp", "Open", true, 2);
    Team openTeam = testDataFactory.createTeam(open);

    mockMvc
        .perform(get("/api/v1/public/competition/{slug}", open.getSlug()))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/v1/public/team/{uuid}", openTeam.getUuid()))
        .andExpect(status().isOk());
  }
}

package org.bytefight.webserver.competition;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.bytefight.webserver.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class PublicCompetitionControllerIT extends FullStackIntegrationTestBase {
  @Autowired private MockMvc mockMvc;

  @Autowired private TestDataFactory testDataFactory;

  @Autowired private CompetitionRepository competitionRepository;

  @Test
  void getAllCompetitionsReturnsEmptyListWhenNoCompetitions() throws Exception {
    mockMvc
        .perform(get("/api/v1/public/competition/"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void getAllCompetitionsReturnsAllCompetitions() throws Exception {
    testDataFactory.createCompetition("comp-1", "Competition One", true, 2);
    testDataFactory.createCompetition("comp-2", "Competition Two", false, 3);

    mockMvc
        .perform(get("/api/v1/public/competition/"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].slug").isArray())
        .andExpect(jsonPath("$[*].slug").value(org.hamcrest.Matchers.hasItems("comp-1", "comp-2")));
  }

  @Test
  void getAllCompetitionsHidesInternalCompetitionsFromAnonymousCallers() throws Exception {
    testDataFactory.createCompetition("comp-public", "Public Competition", true, 2);
    createInternalCompetition("comp-internal");

    mockMvc
        .perform(get("/api/v1/public/competition/"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].slug").value("comp-public"))
        .andExpect(jsonPath("$[0].internal").value(false));
  }

  @Test
  void getAllCompetitionsHidesInternalCompetitionsFromNonAdmins() throws Exception {
    testDataFactory.createCompetition("comp-public", "Public Competition", true, 2);
    createInternalCompetition("comp-internal");
    User nonAdmin = testDataFactory.createUser(null, false);

    mockMvc
        .perform(get("/api/v1/public/competition/").with(user(nonAdmin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].slug").value("comp-public"));
  }

  @Test
  void getAllCompetitionsReturnsInternalCompetitionsForAdmins() throws Exception {
    testDataFactory.createCompetition("comp-public", "Public Competition", true, 2);
    createInternalCompetition("comp-internal");
    User admin = testDataFactory.createUser(null, true);

    mockMvc
        .perform(get("/api/v1/public/competition/").with(user(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(
            jsonPath("$[*].slug")
                .value(org.hamcrest.Matchers.hasItems("comp-public", "comp-internal")));
  }

  private Competition createInternalCompetition(String slug) {
    Competition competition =
        testDataFactory.createCompetition(slug, "Internal Competition", true, 2);
    competition.setInternal(true);
    return competitionRepository.save(competition);
  }
}

package org.bytefight.webserver.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.glicko.domain.TeamStats;
import org.bytefight.webserver.glicko.infra.TeamStatsRepository;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.TeamMember;
import org.bytefight.webserver.team.infra.TeamMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Transactional
class PublicLeaderboardControllerIT extends FullStackIntegrationTestBase {
  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private TestDataFactory testDataFactory;

  @Autowired private TeamStatsRepository teamStatsRepository;

  @Autowired private TeamMemberRepository teamMemberRepository;

  @Test
  void getLadderLeaderboardReturnsMembersAndCorrectDenseRanks() throws Exception {
    Competition competition =
        testDataFactory.createCompetition("test-comp", "Test Competition", true, 3);
    String ladder = "main";
    testDataFactory.createLadder(competition, ladder);

    Team teamA = testDataFactory.createTeam(competition);
    Team teamB = testDataFactory.createTeam(competition);
    Team teamC = testDataFactory.createTeam(competition);

    teamA.setDisplayMembers(true);
    teamB.setDisplayMembers(true);
    teamC.setDisplayMembers(false);

    Player a1 = testDataFactory.createUserWithPlayer("a1@example.com", "alpha1");
    Player a2 = testDataFactory.createUserWithPlayer("a2@example.com", "alpha2");
    Player b1 = testDataFactory.createUserWithPlayer("b1@example.com", "beta1");
    Player c1 = testDataFactory.createUserWithPlayer("c1@example.com", "gamma1");

    teamMemberRepository.save(member(competition, teamA, a1));
    teamMemberRepository.save(member(competition, teamA, a2));
    teamMemberRepository.save(member(competition, teamB, b1));
    teamMemberRepository.save(member(competition, teamC, c1));

    teamStatsRepository.save(teamStats(competition, teamA, ladder, 1600.0, 10));
    teamStatsRepository.save(teamStats(competition, teamB, ladder, 1500.0, 5));
    teamStatsRepository.save(teamStats(competition, teamC, ladder, 1400.0, 0));

    MvcResult result =
        mockMvc
            .perform(
                get(
                    "/api/v1/public/leaderboard/{competitionSlug}/{ladderSlug}",
                    "test-comp",
                    ladder))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode leaderboard = objectMapper.readTree(result.getResponse().getContentAsString());

    assertThat(leaderboard.isArray()).isTrue();
    assertThat(leaderboard).hasSize(3);

    JsonNode first = leaderboard.get(0);
    JsonNode second = leaderboard.get(1);
    JsonNode third = leaderboard.get(2);

    assertThat(first.get("teamUuid").asText())
        .isEqualTo(((java.util.UUID) ReflectionTestUtils.getField(teamA, "uuid")).toString());
    assertThat(first.get("rank").asInt()).isEqualTo(1);
    assertThat(memberUsernames(first.get("members"))).containsExactlyInAnyOrder("alpha1", "alpha2");

    assertThat(second.get("teamUuid").asText())
        .isEqualTo(((java.util.UUID) ReflectionTestUtils.getField(teamB, "uuid")).toString());
    assertThat(second.get("rank").asInt()).isEqualTo(2);
    assertThat(memberUsernames(second.get("members"))).containsExactly("beta1");

    assertThat(third.get("teamUuid").asText())
        .isEqualTo(((java.util.UUID) ReflectionTestUtils.getField(teamC, "uuid")).toString());
    assertThat(third.get("rank").isNull()).isTrue();
    assertThat(memberUsernames(third.get("members"))).isEmpty();
  }

  @Test
  void searchPagesKeepGlobalRanksAndUseTheCompetitionPageSize() throws Exception {
    Competition competition =
        testDataFactory.createCompetition("paged-comp", "Paged Competition", true, 3);
    competition.setLeaderboardPagination(true);
    competition.setLeaderboardPageSize(1);
    String ladder = "ranked";
    testDataFactory.createLadder(competition, ladder);

    Team alpha = namedTeam(competition, "Alpha Wolves");
    Team alphabet = namedTeam(competition, "Alphabet Soup");
    Team beta = namedTeam(competition, "Beta Bears");
    teamStatsRepository.save(teamStats(competition, alpha, ladder, 1800.0, 4));
    teamStatsRepository.save(teamStats(competition, alphabet, ladder, 1600.0, 4));
    teamStatsRepository.save(teamStats(competition, beta, ladder, 1400.0, 4));

    mockMvc
        .perform(
            get("/api/v1/public/leaderboard/{competitionSlug}/{ladderSlug}", "paged-comp", ladder))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));

    mockMvc
        .perform(
            get(
                    "/api/v1/public/leaderboard/{competitionSlug}/{ladderSlug}/page",
                    "paged-comp",
                    ladder)
                .param("page", "0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].teamName").value("Alpha Wolves"))
        .andExpect(jsonPath("$.content[0].rank").value(1))
        .andExpect(jsonPath("$.page.size").value(1))
        .andExpect(jsonPath("$.page.number").value(0))
        .andExpect(jsonPath("$.page.totalElements").value(3))
        .andExpect(jsonPath("$.page.totalPages").value(3));

    mockMvc
        .perform(
            get(
                    "/api/v1/public/leaderboard/{competitionSlug}/{ladderSlug}/page",
                    "paged-comp",
                    ladder)
                .param("page", "1")
                .param("search", "  ALPHA "))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].teamName").value("Alphabet Soup"))
        .andExpect(jsonPath("$.content[0].rank").value(2))
        .andExpect(jsonPath("$.page.totalElements").value(2))
        .andExpect(jsonPath("$.page.totalPages").value(2));

    mockMvc
        .perform(
            get(
                    "/api/v1/public/leaderboard/{competitionSlug}/{ladderSlug}/page",
                    "paged-comp",
                    ladder)
                .param("search", "nope"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.page.totalElements").value(0))
        .andExpect(jsonPath("$.page.totalPages").value(0));
  }

  @Test
  void disabledPaginationIgnoresPageIndexAndClientPageSize() throws Exception {
    Competition competition =
        testDataFactory.createCompetition("single-comp", "Single Competition", true, 3);
    competition.setLeaderboardPagination(false);
    competition.setLeaderboardPageSize(1);
    String ladder = "ranked";
    testDataFactory.createLadder(competition, ladder);
    Team alpha = namedTeam(competition, "Alpha Wolves");
    Team beta = namedTeam(competition, "Beta Bears");
    teamStatsRepository.save(teamStats(competition, alpha, ladder, 1800.0, 4));
    teamStatsRepository.save(teamStats(competition, beta, ladder, 1400.0, 4));

    mockMvc
        .perform(
            get(
                    "/api/v1/public/leaderboard/{competitionSlug}/{ladderSlug}/page",
                    "single-comp",
                    ladder)
                .param("page", "5")
                .param("size", "1")
                .param("search", "wolves"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].teamName").value("Alpha Wolves"))
        .andExpect(jsonPath("$.page.number").value(0))
        .andExpect(jsonPath("$.page.totalPages").value(1))
        .andExpect(jsonPath("$.page.totalElements").value(1));
  }

  @Test
  void pageKeepsMembersRanksAndIgnoresOtherLaddersQuotesAndDeletedTeams() throws Exception {
    Competition competition =
        testDataFactory.createCompetition("scoped-comp", "Scoped Competition", true, 3);
    competition.setLeaderboardPagination(true);
    competition.setLeaderboardPageSize(1);
    String ladder = "ranked";
    testDataFactory.createLadder(competition, ladder);
    testDataFactory.createLadder(competition, "scrimmage");

    Competition other =
        testDataFactory.createCompetition("other-comp", "Other Competition", true, 3);
    testDataFactory.createLadder(other, ladder);

    Team visible = namedTeam(competition, "Visible Vipers");
    visible.setDisplayMembers(true);
    visible.setQuote("nothing to see");
    Team hidden = namedTeam(competition, "Hidden Hawks");
    hidden.setDisplayMembers(false);
    hidden.setQuote("visible is only in the quote");
    Team unranked = namedTeam(competition, "Unranked Umbrellas");
    Team deleted = namedTeam(competition, "Deleted Dynamos");
    deleted.softDelete();
    Team otherLadder = namedTeam(competition, "Scrim Sharks");
    Team foreign = namedTeam(other, "Foreign Foxes");

    Player member = testDataFactory.createUserWithPlayer("visible@example.com", "viper");
    Player hiddenMember = testDataFactory.createUserWithPlayer("hidden@example.com", "hawk");
    teamMemberRepository.save(member(competition, visible, member));
    teamMemberRepository.save(member(competition, hidden, hiddenMember));

    teamStatsRepository.save(teamStats(competition, visible, ladder, 1800.0, 3));
    teamStatsRepository.save(teamStats(competition, hidden, ladder, 1500.0, 3));
    teamStatsRepository.save(teamStats(competition, unranked, ladder, 2000.0, 0));
    teamStatsRepository.save(teamStats(competition, deleted, ladder, 1900.0, 9));
    teamStatsRepository.save(teamStats(competition, otherLadder, "scrimmage", 1700.0, 9));
    teamStatsRepository.save(teamStats(other, foreign, ladder, 1700.0, 9));

    mockMvc
        .perform(
            get(
                    "/api/v1/public/leaderboard/{competitionSlug}/{ladderSlug}/page",
                    "scoped-comp",
                    ladder)
                .param("page", "0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].teamName").value("Visible Vipers"))
        .andExpect(jsonPath("$.content[0].rank").value(1))
        .andExpect(jsonPath("$.content[0].members[0].username").value("viper"))
        .andExpect(jsonPath("$.page.totalElements").value(3));

    mockMvc
        .perform(
            get(
                    "/api/v1/public/leaderboard/{competitionSlug}/{ladderSlug}/page",
                    "scoped-comp",
                    ladder)
                .param("page", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].teamName").value("Hidden Hawks"))
        .andExpect(jsonPath("$.content[0].rank").value(2))
        .andExpect(jsonPath("$.content[0].members").value(org.hamcrest.Matchers.nullValue()));

    mockMvc
        .perform(
            get(
                    "/api/v1/public/leaderboard/{competitionSlug}/{ladderSlug}/page",
                    "scoped-comp",
                    ladder)
                .param("page", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].teamName").value("Unranked Umbrellas"))
        .andExpect(jsonPath("$.content[0].rank").value(org.hamcrest.Matchers.nullValue()));

    mockMvc
        .perform(
            get(
                    "/api/v1/public/leaderboard/{competitionSlug}/{ladderSlug}/page",
                    "scoped-comp",
                    ladder)
                .param("search", "visible"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].teamName").value("Visible Vipers"));
  }

  @Test
  void missingCompetitionIsNotFound() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/public/leaderboard/{competitionSlug}/{ladderSlug}", "missing", "ranked"))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get(
                "/api/v1/public/leaderboard/{competitionSlug}/{ladderSlug}/page",
                "missing",
                "ranked"))
        .andExpect(status().isNotFound());
  }

  private Team namedTeam(Competition competition, String name) {
    Team team = testDataFactory.createTeam(competition);
    team.setName(name);
    return team;
  }

  private static TeamStats teamStats(
      Competition competition, Team team, String ladder, double rating, int matchesPlayed) {
    TeamStats stats = new TeamStats();
    ReflectionTestUtils.setField(stats, "competition", competition);
    ReflectionTestUtils.setField(stats, "team", team);
    ReflectionTestUtils.setField(stats, "ladder", ladder);
    ReflectionTestUtils.setField(stats, "glickoRating", rating);
    ReflectionTestUtils.setField(stats, "matchesPlayed", matchesPlayed);
    ReflectionTestUtils.setField(stats, "wins", 0);
    ReflectionTestUtils.setField(stats, "losses", 0);
    ReflectionTestUtils.setField(stats, "draws", 0);
    ReflectionTestUtils.setField(stats, "glickoRd", 350.0);
    ReflectionTestUtils.setField(stats, "glickoVolatility", 0.06);
    return stats;
  }

  private static TeamMember member(Competition competition, Team team, Player player) {
    TeamMember member = new TeamMember();
    ReflectionTestUtils.setField(member, "competition", competition);
    ReflectionTestUtils.setField(member, "team", team);
    ReflectionTestUtils.setField(member, "player", player);
    return member;
  }

  private static List<String> memberUsernames(JsonNode membersNode) {
    List<String> usernames = new ArrayList<>();
    if (membersNode == null || !membersNode.isArray()) {
      return usernames;
    }
    for (JsonNode member : membersNode) {
      usernames.add(member.get("username").asText());
    }
    return usernames;
  }
}

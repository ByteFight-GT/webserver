package org.bytefight.webserver.leaderboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PublicLeaderboardControllerIT extends FullStackIntegrationTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private TeamStatsRepository teamStatsRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Test
    void getLadderLeaderboardReturnsMembersAndCorrectDenseRanks() throws Exception {
        Competition competition = testDataFactory.createCompetition("test-comp", "Test Competition", true, 3);
        String ladder = "main";
        testDataFactory.createLadder(competition, ladder);

        Team teamA = testDataFactory.createTeam(competition);
        Team teamB = testDataFactory.createTeam(competition);
        Team teamC = testDataFactory.createTeam(competition);

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

        MvcResult result = mockMvc.perform(get("/api/v1/public/leaderboard/{competitionSlug}/{ladderSlug}", "test-comp", ladder))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode leaderboard = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(leaderboard.isArray()).isTrue();
        assertThat(leaderboard).hasSize(3);

        JsonNode first = leaderboard.get(0);
        JsonNode second = leaderboard.get(1);
        JsonNode third = leaderboard.get(2);

        assertThat(first.get("teamUuid").asText()).isEqualTo(teamA.getUuid().toString());
        assertThat(first.get("rank").asInt()).isEqualTo(1);
        assertThat(memberUsernames(first.get("members"))).containsExactlyInAnyOrder("alpha1", "alpha2");

        assertThat(second.get("teamUuid").asText()).isEqualTo(teamB.getUuid().toString());
        assertThat(second.get("rank").asInt()).isEqualTo(2);
        assertThat(memberUsernames(second.get("members"))).containsExactly("beta1");

        assertThat(third.get("teamUuid").asText()).isEqualTo(teamC.getUuid().toString());
        assertThat(third.get("rank").isNull()).isTrue();
        assertThat(memberUsernames(third.get("members"))).containsExactly("gamma1");
    }

    private static TeamStats teamStats(Competition competition, Team team, String ladder, double rating, int matchesPlayed) {
        TeamStats stats = new TeamStats();
        stats.setCompetition(competition);
        stats.setTeam(team);
        stats.setLadder(ladder);
        stats.setGlickoRating(rating);
        stats.setMatchesPlayed(matchesPlayed);
        stats.setWins(0);
        stats.setLosses(0);
        stats.setDraws(0);
        stats.setGlickoRd(350.0);
        stats.setGlickoVolatility(0.06);
        return stats;
    }

    private static TeamMember member(Competition competition, Team team, Player player) {
        TeamMember member = new TeamMember();
        member.setCompetition(competition);
        member.setTeam(team);
        member.setPlayer(player);
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

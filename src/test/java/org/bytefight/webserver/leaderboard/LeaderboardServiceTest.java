package org.bytefight.webserver.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.glicko.infra.TeamStatsRepository;
import org.bytefight.webserver.leaderboard.application.LeaderboardService;
import org.bytefight.webserver.leaderboard.domain.LeaderboardDto;
import org.bytefight.webserver.leaderboard.domain.LeaderboardRow;
import org.bytefight.webserver.team.infra.TeamMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

class LeaderboardServiceTest {
  private TeamStatsRepository teamStatsRepository;
  private TeamMemberRepository teamMemberRepository;
  private LeaderboardService leaderboardService;

  @BeforeEach
  void setUp() {
    teamStatsRepository = org.mockito.Mockito.mock(TeamStatsRepository.class);
    teamMemberRepository = org.mockito.Mockito.mock(TeamMemberRepository.class);
    leaderboardService = new LeaderboardService(teamStatsRepository, teamMemberRepository);
    when(teamMemberRepository.findMemberSummariesByTeamUuids(
            org.mockito.ArgumentMatchers.anyList()))
        .thenReturn(List.of());
  }

  @Test
  void searchFiltersAfterRankingAndUsesTheCompetitionPageSize() {
    Competition competition = new Competition();
    competition.setLeaderboardPagination(true);
    competition.setLeaderboardPageSize(1);
    when(teamStatsRepository.findLeaderboardRowsByCompetitionAndLadder(competition, "ranked"))
        .thenReturn(
            List.of(
                row("alpha", "Alpha Wolves", 1),
                row("alphabet", "Alphabet Soup", 2),
                row("beta", "Beta Bears", 3)));

    Page<LeaderboardDto> secondAlpha =
        leaderboardService.searchLeaderboard(competition, "ranked", "  ALPHA ", 1);

    assertThat(secondAlpha.getTotalElements()).isEqualTo(2);
    assertThat(secondAlpha.getTotalPages()).isEqualTo(2);
    assertThat(secondAlpha.getSize()).isEqualTo(1);
    assertThat(secondAlpha.getNumber()).isEqualTo(1);
    assertThat(secondAlpha.getContent()).hasSize(1);
    assertThat(secondAlpha.getContent().get(0).getTeamName()).isEqualTo("Alphabet Soup");
    assertThat(secondAlpha.getContent().get(0).getRank()).isEqualTo(2);

    Page<LeaderboardDto> empty =
        leaderboardService.searchLeaderboard(competition, "ranked", "nope", 0);
    assertThat(empty.getContent()).isEmpty();
    assertThat(empty.getTotalElements()).isZero();
    assertThat(empty.getTotalPages()).isZero();
  }

  @Test
  void disabledPaginationReturnsEveryMatchOnOnePage() {
    Competition competition = new Competition();
    competition.setLeaderboardPagination(false);
    competition.setLeaderboardPageSize(1);
    when(teamStatsRepository.findLeaderboardRowsByCompetitionAndLadder(competition, "ranked"))
        .thenReturn(List.of(row("a", "Alpha", 1), row("b", "Beta", 2), row("c", "Gamma", 3)));

    Page<LeaderboardDto> page =
        leaderboardService.searchLeaderboard(competition, "ranked", "alpha", 4);

    assertThat(page.getNumber()).isZero();
    assertThat(page.getTotalPages()).isEqualTo(1);
    assertThat(page.getContent()).extracting(LeaderboardDto::getTeamName).containsExactly("Alpha");
  }

  @Test
  void pagePastTheEndIsEmptyAndNegativePageClampsToZero() {
    Competition competition = new Competition();
    competition.setLeaderboardPagination(true);
    competition.setLeaderboardPageSize(2);
    when(teamStatsRepository.findLeaderboardRowsByCompetitionAndLadder(competition, "ranked"))
        .thenReturn(List.of(row("a", "Alpha", 1), row("b", "Beta", 2), row("c", "Gamma", 3)));

    Page<LeaderboardDto> first =
        leaderboardService.searchLeaderboard(competition, "ranked", null, -5);
    assertThat(first.getNumber()).isZero();
    assertThat(first.getContent())
        .extracting(LeaderboardDto::getTeamName)
        .containsExactly("Alpha", "Beta");

    Page<LeaderboardDto> pastEnd =
        leaderboardService.searchLeaderboard(competition, "ranked", "", 9);
    assertThat(pastEnd.getContent()).isEmpty();
    assertThat(pastEnd.getTotalElements()).isEqualTo(3);
    assertThat(pastEnd.getTotalPages()).isEqualTo(2);
  }

  private static LeaderboardRow row(String uuid, String name, int rank) {
    return new LeaderboardRow() {
      @Override
      public String getTeamUuid() {
        return uuid;
      }

      @Override
      public String getTeamName() {
        return name;
      }

      @Override
      public String getTeamQuote() {
        return "quote";
      }

      @Override
      public String getTeamType() {
        return "regular";
      }

      @Override
      public Double getGlickoRating() {
        return 1500.0;
      }

      @Override
      public Integer getRank() {
        return rank;
      }

      @Override
      public boolean getTeamDisplayMembers() {
        return false;
      }
    };
  }
}

package org.bytefight.webserver.leaderboard.application;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.glicko.infra.TeamStatsRepository;
import org.bytefight.webserver.leaderboard.domain.LeaderboardDto;
import org.bytefight.webserver.leaderboard.domain.LeaderboardRow;
import org.bytefight.webserver.leaderboard.domain.MemberSummary;
import org.bytefight.webserver.leaderboard.domain.MemberSummaryDto;
import org.bytefight.webserver.team.domain.TeamType;
import org.bytefight.webserver.team.infra.TeamMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardService {
  private final TeamStatsRepository teamStatsRepository;
  private final TeamMemberRepository teamMemberRepository;

  public LeaderboardService(
      TeamStatsRepository teamStatsRepository, TeamMemberRepository teamMemberRepository) {
    this.teamStatsRepository = teamStatsRepository;
    this.teamMemberRepository = teamMemberRepository;
  }

  public List<LeaderboardDto> getFullLeaderboardByCompetitionAndLadder(
      Competition competition, String ladder) {
    return toDtos(
        teamStatsRepository.findLeaderboardRowsByCompetitionAndLadder(competition, ladder));
  }

  /**
   * Search and page a ladder. Ranks stay global: filtering happens after the rank window, and only
   * the current page's members are loaded. Page size comes from the competition setting.
   */
  public Page<LeaderboardDto> searchLeaderboard(
      Competition competition, String ladder, String search, int page) {
    List<LeaderboardRow> rows =
        teamStatsRepository.findLeaderboardRowsByCompetitionAndLadder(competition, ladder);
    String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    if (!query.isEmpty()) {
      rows =
          rows.stream()
              .filter(
                  row ->
                      row.getTeamName() != null
                          && row.getTeamName().toLowerCase(Locale.ROOT).contains(query))
              .toList();
    }

    int total = rows.size();
    boolean paginate = competition.isLeaderboardPagination();
    int size = paginate ? Math.max(competition.getLeaderboardPageSize(), 1) : Math.max(total, 1);
    int safePage = paginate ? Math.max(page, 0) : 0;
    int fromIndex = 0;
    int toIndex = total;
    if (paginate) {
      long fromLong = (long) safePage * size;
      fromIndex = fromLong >= total ? total : (int) fromLong;
      toIndex = Math.min(fromIndex + size, total);
    }
    List<LeaderboardDto> content = toDtos(List.copyOf(rows.subList(fromIndex, toIndex)));
    return new PageImpl<>(content, PageRequest.of(safePage, size), total);
  }

  private List<LeaderboardDto> toDtos(List<LeaderboardRow> rows) {
    if (rows.isEmpty()) {
      return List.of();
    }

    List<String> visibleTeamUuids =
        rows.stream()
            .filter(LeaderboardRow::getTeamDisplayMembers)
            .map(LeaderboardRow::getTeamUuid)
            .toList();
    final Map<String, List<MemberSummaryDto>> membersByTeamUuid;
    if (visibleTeamUuids.isEmpty()) {
      membersByTeamUuid = Map.of();
    } else {
      List<MemberSummary> memberSummaries =
          teamMemberRepository.findMemberSummariesByTeamUuids(visibleTeamUuids);
      membersByTeamUuid =
          memberSummaries.stream()
              .collect(
                  Collectors.groupingBy(
                      MemberSummary::getTeamUuid,
                      Collectors.mapping(
                          summary ->
                              new MemberSummaryDto(
                                  summary.getUuid(), summary.getUsername(), summary.getIsDev()),
                          Collectors.toList())));
    }

    return rows.stream()
        .map(
            row ->
                new LeaderboardDto(
                    row.getTeamUuid(),
                    row.getTeamName(),
                    row.getTeamQuote(),
                    TeamType.valueOf(row.getTeamType()),
                    row.getGlickoRating(),
                    row.getRank(),
                    row.getTeamDisplayMembers()
                        ? membersByTeamUuid.getOrDefault(row.getTeamUuid(), List.of())
                        : null))
        .toList();
  }
}

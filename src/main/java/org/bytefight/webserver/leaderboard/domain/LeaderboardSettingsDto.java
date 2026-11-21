package org.bytefight.webserver.leaderboard.domain;

import org.bytefight.webserver.competition.domain.Competition;

public record LeaderboardSettingsDto(
    Long competitionId, boolean leaderboardPagination, int leaderboardPageSize) {
  public static LeaderboardSettingsDto from(Competition competition) {
    return new LeaderboardSettingsDto(
        competition.getId(),
        competition.isLeaderboardPagination(),
        competition.getLeaderboardPageSize());
  }
}

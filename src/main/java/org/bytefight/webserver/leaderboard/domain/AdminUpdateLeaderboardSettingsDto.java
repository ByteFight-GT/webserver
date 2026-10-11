package org.bytefight.webserver.leaderboard.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Value;

import org.bytefight.webserver.competition.domain.Competition;

@Value
public class AdminUpdateLeaderboardSettingsDto {
  Boolean leaderboardPagination;

  @Min(1)
  @Max(Competition.MAX_LEADERBOARD_PAGE_SIZE)
  Integer leaderboardPageSize;
}

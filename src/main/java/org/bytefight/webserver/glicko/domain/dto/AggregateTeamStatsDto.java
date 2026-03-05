package org.bytefight.webserver.glicko.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import org.bytefight.webserver.glicko.domain.TeamStats;

@Value
@Builder
public class AggregateTeamStatsDto {
  @NotNull String teamUuid;
  @NotNull String competitionSlug;

  // Primitives cannot be null, so @NotNull is omitted here
  int matchesPlayed;
  int wins;
  int losses;
  int draws;

  public static AggregateTeamStatsDto from(TeamStats stats) {
    return AggregateTeamStatsDto.builder()
        .teamUuid(stats.getTeam().getUuid().toString())
        .competitionSlug(stats.getCompetition().getSlug())
        .matchesPlayed(stats.getMatchesPlayed())
        .wins(stats.getWins())
        .losses(stats.getLosses())
        .draws(stats.getDraws())
        .build();
  }
}

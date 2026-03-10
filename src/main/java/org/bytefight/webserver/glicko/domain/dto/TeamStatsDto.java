package org.bytefight.webserver.glicko.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import org.bytefight.webserver.glicko.domain.TeamStats;
import org.bytefight.webserver.glicko.domain.TeamStatsAggregate;

@Value
@Builder
public class TeamStatsDto {
  @NotNull String teamUuid;
  @NotNull String competitionSlug;
  String ladder;

  // Primitives cannot be null, so @NotNull is omitted here
  @NotNull Integer matchesPlayed;
  @NotNull Integer wins;
  @NotNull Integer losses;
  @NotNull Integer draws;

  Double glickoRating;
  Double glickoRd;
  Double glickoVolatility;

  public static TeamStatsDto from(TeamStats stats) {
    return TeamStatsDto.builder()
        .teamUuid(stats.getTeam().getUuid().toString())
        .competitionSlug(stats.getCompetition().getSlug())
        .ladder(stats.getLadder())
        .matchesPlayed(stats.getMatchesPlayed())
        .wins(stats.getWins())
        .losses(stats.getLosses())
        .draws(stats.getDraws())
        .glickoRating(stats.getGlickoRating())
        .glickoRd(stats.getGlickoRd())
        .glickoVolatility(stats.getGlickoVolatility())
        .build();
  }

  public static TeamStatsDto from(TeamStatsAggregate teamStatsAggregate) {
    return TeamStatsDto.builder()
        .teamUuid(teamStatsAggregate.getTeam().getUuid().toString())
        .competitionSlug(teamStatsAggregate.getTeam().getCompetition().getSlug())
        .matchesPlayed(teamStatsAggregate.getMatchesPlayed())
        .wins(teamStatsAggregate.getWins())
        .losses(teamStatsAggregate.getLosses())
        .draws(teamStatsAggregate.getDraws())
        .build();
  }
}

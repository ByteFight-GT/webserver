package org.bytefight.webserver.glicko.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import org.bytefight.webserver.glicko.domain.TeamStats;

@Value
@Builder
public class TeamStatsDto {
  @NotNull String teamUuid;
  @NotNull String competitionSlug;
  @NotNull String ladder;

  // Primitives cannot be null, so @NotNull is omitted here
  int matchesPlayed;
  int wins;
  int losses;
  int draws;
  double glickoRating;
  double glickoRd;
  double glickoVolatility;

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
}

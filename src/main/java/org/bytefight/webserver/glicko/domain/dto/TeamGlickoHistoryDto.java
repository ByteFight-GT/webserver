package org.bytefight.webserver.glicko.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.glicko.domain.TeamGlickoHistory;
import org.bytefight.webserver.team.domain.Team;

@Value
@Builder
public class TeamGlickoHistoryDto {
  @NotNull String teamUuid;
  @NotNull String competitionSlug;
  @NotNull String ladder;
  @NotNull List<Long> epochSecond;
  @NotNull List<Double> glicko;

  public static TeamGlickoHistoryDto from(
      Competition competition, Team team, String ladder, List<TeamGlickoHistory> histories) {
    List<Long> timestamps = new ArrayList<>(histories.size());
    List<Double> glickos = new ArrayList<>(histories.size());

    if (!histories.isEmpty()) {
      TeamGlickoHistory initial = histories.get(0);
      timestamps.add(team.getCreatedAt().getEpochSecond());
      glickos.add(initial.getOldGlicko());
    }

    for (TeamGlickoHistory h : histories) {
      timestamps.add(h.getCreatedAt().getEpochSecond());
      glickos.add(h.getNewGlicko());
    }

    return TeamGlickoHistoryDto.builder()
        .teamUuid(team.getUuid().toString())
        .competitionSlug(competition.getSlug())
        .ladder(ladder)
        .epochSecond(timestamps)
        .glicko(glickos)
        .build();
  }
}

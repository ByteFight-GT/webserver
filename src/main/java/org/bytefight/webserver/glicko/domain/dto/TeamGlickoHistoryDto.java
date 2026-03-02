package org.bytefight.webserver.glicko.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.bytefight.webserver.glicko.domain.TeamGlickoHistory;

@Value
@Builder
public class TeamGlickoHistoryDto {
  @NotNull String teamUuid;
  @NotNull String competitionSlug;
  @NotNull String ladder;
  @NotNull Instant recordedAt;

  @NotNull double oldGlicko;
  @NotNull double newGlicko;

  public static TeamGlickoHistoryDto from(TeamGlickoHistory history) {
    return TeamGlickoHistoryDto.builder()
        .teamUuid(history.getTeam().getUuid().toString())
        .competitionSlug(history.getCompetition().getSlug())
        .ladder(history.getLadder())
        .recordedAt(history.getCreatedAt()) // Inherited from BaseEntity
        .oldGlicko(history.getOldGlicko())
        .newGlicko(history.getNewGlicko())
        .build();
  }

  // Method to convert a list of TeamGlickoHistory into a list of TeamGlickoHistoryDto
  public static List<TeamGlickoHistoryDto> listFrom(List<TeamGlickoHistory> historyList) {
    return historyList.stream()
        .map(TeamGlickoHistoryDto::from) // Convert each TeamGlickoHistory to TeamGlickoHistoryDto
        .collect(Collectors.toList()); // Collect them into a List
  }

  // Method to convert a list of TeamGlickoHistory into an array of TeamGlickoHistoryDto
  public static TeamGlickoHistoryDto[] arrayFrom(List<TeamGlickoHistory> historyList) {
    return historyList.stream()
        .map(TeamGlickoHistoryDto::from) // Convert each TeamGlickoHistory to TeamGlickoHistoryDto
        .toArray(TeamGlickoHistoryDto[]::new); // Collect them into an array
  }
}

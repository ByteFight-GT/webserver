package org.bytefight.webserver.profile.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import org.bytefight.webserver.profile.domain.ProfileStats;

@Value
@Builder
public class ProfileStatsDto {
  @NotNull Integer gamesPlayed;
  Double gamesPlayedPercentile;

  public static ProfileStatsDto from(ProfileStats stats) {
    return ProfileStatsDto.builder()
        .gamesPlayed(stats.getGamesPlayed())
        .gamesPlayedPercentile(stats.getGamesPlayedPercentile())
        .build();
  }

  public static ProfileStatsDto notYetComputed() {
    return ProfileStatsDto.builder().gamesPlayed(0).build();
  }
}

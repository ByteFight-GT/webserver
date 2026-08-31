package org.bytefight.webserver.profile.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.util.List;

@Value
public class PlayerCompetitionDto {
  @NotNull String competitionSlug;
  @NotNull String competitionName;
  @NotNull String teamName;
  @NotNull String teamUuid;
  List<PlayerCompetitionMemberDto> teamMembers;
  Integer leaderboardRank;

  @Value
  public static class PlayerCompetitionMemberDto {
    @NotNull String uuid;
    @NotNull String username;
  }
}

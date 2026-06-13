package org.bytefight.webserver.competition.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.util.List;

@Value
public class PlayerCompetitionDto {
  @NotNull String competitionSlug;
  @NotNull String competitionName;
  @NotNull String teamName;
  @NotNull String teamUuid;
  @NotNull List<MemberDto> members;
  @NotNull List<RankingDto> rankings;

  @Value
  public static class MemberDto {
    @NotNull String uuid;
    @NotNull String username;
  }

  @Value
  public static class RankingDto {
    @NotNull String ladder;
    Integer rank;
  }
}

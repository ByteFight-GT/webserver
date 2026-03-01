package org.bytefight.webserver.leaderboard.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.util.List;

import org.bytefight.webserver.team.domain.TeamType;

@Value
public class LeaderboardDto {
  @NotNull String teamUuid;
  @NotNull String teamName;
  @NotNull String teamQuote;
  @NotNull TeamType type;

  @NotNull double glicko;
  Integer rank;
  List<MemberSummaryDto> members;

  public LeaderboardDto(
      String teamUuid,
      String teamName,
      String teamQuote,
      TeamType type,
      double glicko,
      Integer rank,
      List<MemberSummaryDto> members) {
    this.teamUuid = teamUuid;
    this.teamName = teamName;
    this.teamQuote = teamQuote;
    this.type = type;
    this.glicko = glicko;
    this.rank = rank;
    this.members = members;
  }
}

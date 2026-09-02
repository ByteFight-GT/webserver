package org.bytefight.webserver.profile.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;
import org.bytefight.webserver.leaderboard.domain.MemberSummaryDto;

import java.util.List;

@Value
public class PlayerCompetitionDto {
  @NotNull String competitionSlug;
  @NotNull String competitionName;
  @NotNull String teamName;
  @NotNull String teamUuid;
  List<MemberSummaryDto> teamMembers;
  Integer leaderboardRank;
}

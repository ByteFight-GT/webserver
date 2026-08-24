package org.bytefight.webserver.gamematch.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.time.Instant;

import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;

@Value
public class AdminGameMatchDto {
  @NotNull Long id;
  @NotNull String uuid;
  @NotNull Long competitionId;
  @NotNull String ladder;
  @NotNull Long teamAId;
  @NotNull Long teamBId;
  @NotNull String teamAUuid;
  @NotNull String teamBUuid;
  @NotNull Long submissionAId;
  @NotNull Long submissionBId;
  MatchStatus status;
  MatchReason reason;
  String mapCode;
  String outcomeReasonCode;
  Instant scheduledAt;
  Instant startedAt;
  Instant finishedAt;
  @NotNull Instant createdAt;

  public static AdminGameMatchDto fromEntity(GameMatch gameMatch) {
    return new AdminGameMatchDto(
        gameMatch.getId(),
        gameMatch.getUuid().toString(),
        gameMatch.getCompetition().getId(),
        gameMatch.getLadder(),
        gameMatch.getTeamA().getId(),
        gameMatch.getTeamB().getId(),
        gameMatch.getTeamA().getUuid().toString(),
        gameMatch.getTeamB().getUuid().toString(),
        gameMatch.getSubmissionA().getId(),
        gameMatch.getSubmissionB().getId(),
        gameMatch.getStatus(),
        gameMatch.getReason(),
        gameMatch.getMapCode(),
        gameMatch.getOutcomeReasonCode(),
        gameMatch.getScheduledAt(),
        gameMatch.getStartedAt(),
        gameMatch.getFinishedAt(),
        gameMatch.getCreatedAt());
  }
}

package org.bytefight.webserver.gamematch.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

import org.bytefight.webserver.common.domain.dto.TimestampsDto;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameMatchDto {
  @NotNull private String uuid;
  @NotNull private String competitionSlug;
  @NotNull private String teamAName;
  @NotNull private String teamBName;
  @NotNull private String teamAUuid;
  @NotNull private String teamBUuid;
  @NotNull private String submissionAName;
  @NotNull private String submissionBName;
  @NotNull private Map<String, Object> matchSettings;
  private MatchStatus status;
  private MatchReason reason;
  private String mapCode;
  private String outcomeReasonCode;
  @NotNull private Instant scheduledAt;
  @NotNull private Instant startedAt;
  @NotNull private Instant finishedAt;
  @NotNull TimestampsDto timestampsDto;

  // Convert from Entity to DTO
  public static GameMatchDto fromEntity(GameMatch gameMatch) {
    return GameMatchDto.builder()
        .uuid(gameMatch.getUuid().toString())
        .competitionSlug(gameMatch.getCompetition().getSlug())
        .teamAName(gameMatch.getTeamA().getName())
        .teamBName(gameMatch.getTeamB().getName())
        .teamAUuid(gameMatch.getTeamA().getUuid().toString())
        .teamBUuid(gameMatch.getTeamB().getUuid().toString())
        .submissionAName(gameMatch.getSubmissionA().getFileRecord().getFilename())
        .submissionBName(gameMatch.getSubmissionB().getFileRecord().getFilename())
        .matchSettings(gameMatch.getMatchSettings())
        .status(gameMatch.getStatus())
        .reason(gameMatch.getReason())
        .mapCode(gameMatch.getMapCode())
        .outcomeReasonCode(gameMatch.getOutcomeReasonCode())
        .scheduledAt(gameMatch.getScheduledAt())
        .startedAt(gameMatch.getStartedAt())
        .finishedAt(gameMatch.getFinishedAt())
        .timestampsDto(TimestampsDto.from(gameMatch))
        .build();
  }
}

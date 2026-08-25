package org.bytefight.webserver.scrim.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchStatus;

/**
 * Result of a scrim request. Deliberately omits the TA bot's identity — the student named it by
 * slug, and the opponent's UUID/name must not leak back.
 */
@Data
@Builder
public class ScrimResponseDto {
  private String taBotSlug;
  private List<ScrimMatchDto> scheduled;
  private int remainingDaily;
  private int remainingWeekly;

  @Data
  @Builder
  public static class ScrimMatchDto {
    private String matchUuid;
    private MatchStatus status;
    private Instant scheduledAt;

    public static ScrimMatchDto from(GameMatch match) {
      return ScrimMatchDto.builder()
          .matchUuid(match.getUuid().toString())
          .status(match.getStatus())
          .scheduledAt(match.getScheduledAt())
          .build();
    }
  }
}

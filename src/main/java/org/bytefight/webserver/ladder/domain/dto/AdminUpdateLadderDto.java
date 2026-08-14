package org.bytefight.webserver.ladder.domain.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Value;

import org.bytefight.webserver.common.validation.ValidCron;

@Value
public class AdminUpdateLadderDto {
  @Positive Double glickoDefaultRating;
  @Positive Double glickoDefaultRd;
  @Positive Double glickoRdMax;
  @Positive Double glickoRdMin;
  @PositiveOrZero Double glickoPhiInflationPerDay;
  @Positive Double glickoTau;
  @Positive Double glickoSigmaDefault;
  @Positive Double glickoSigmaMin;
  @Positive Double glickoSigmaMax;
  @Positive Integer maxQueuedPerTeam;
  Boolean allowUserMatches;
  Boolean scheduledMatchmakingEnabled;
  @ValidCron String scheduledMatchmakingCron;
}

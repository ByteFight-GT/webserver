package org.bytefight.webserver.ladder.domain.dto;

import lombok.Value;

import org.bytefight.webserver.ladder.domain.Ladder;

@Value
public class AdminLadderDto {
  Long id;
  String ladder;
  Long competitionId;
  boolean allowUserMatches;
  double glickoDefaultRating;
  double glickoDefaultRd;
  double glickoRdMax;
  Double glickoRdMin;
  double glickoPhiInflationPerDay;
  double glickoTau;
  double glickoSigmaDefault;
  Double glickoSigmaMin;
  Double glickoSigmaMax;
  boolean scheduledMatchmakingEnabled;
  String scheduledMatchmakingCron;

  public static AdminLadderDto from(Ladder ladder) {
    return new AdminLadderDto(
        ladder.getId(),
        ladder.getLadder(),
        ladder.getCompetition().getId(),
        ladder.isAllowUserMatches(),
        ladder.getGlickoDefaultRating(),
        ladder.getGlickoDefaultRd(),
        ladder.getGlickoRdMax(),
        ladder.getGlickoRdMin(),
        ladder.getGlickoPhiInflationPerDay(),
        ladder.getGlickoTau(),
        ladder.getGlickoSigmaDefault(),
        ladder.getGlickoSigmaMin(),
        ladder.getGlickoSigmaMax(),
        ladder.isScheduledMatchmakingEnabled(),
        ladder.getScheduledMatchmakingCron());
  }
}

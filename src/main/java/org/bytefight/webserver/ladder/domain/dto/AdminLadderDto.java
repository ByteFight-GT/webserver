package org.bytefight.webserver.ladder.domain.dto;

import lombok.Value;

import org.bytefight.webserver.ladder.domain.Ladder;

@Value
public class AdminLadderDto {
  Long id;
  String ladder;
  Long competitionId;
  double glickoDefaultRating;
  double glickoDefaultRd;
  double glickoRdMax;
  Double glickoRdMin;
  double glickoPhiInflationPerDay;
  double glickoTau;
  double glickoSigmaDefault;
  Double glickoSigmaMin;
  Double glickoSigmaMax;

  public static AdminLadderDto from(Ladder ladder) {
    return new AdminLadderDto(
        ladder.getId(),
        ladder.getLadder(),
        ladder.getCompetition().getId(),
        ladder.getGlickoDefaultRating(),
        ladder.getGlickoDefaultRd(),
        ladder.getGlickoRdMax(),
        ladder.getGlickoRdMin(),
        ladder.getGlickoPhiInflationPerDay(),
        ladder.getGlickoTau(),
        ladder.getGlickoSigmaDefault(),
        ladder.getGlickoSigmaMin(),
        ladder.getGlickoSigmaMax());
  }
}

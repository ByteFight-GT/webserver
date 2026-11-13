package org.bytefight.webserver.gamematch.domain.dto;

import lombok.Value;

import org.bytefight.webserver.gamematch.domain.GameOutcomeReason;

@Value
public class AdminGameOutcomeReasonDto {
  Long id;
  Long competitionId;
  String code;
  String displayLabel;
  boolean visible;

  public static AdminGameOutcomeReasonDto from(GameOutcomeReason outcomeReason) {
    return new AdminGameOutcomeReasonDto(
        outcomeReason.getId(),
        outcomeReason.getCompetition().getId(),
        outcomeReason.getCode(),
        outcomeReason.getDisplayLabel(),
        outcomeReason.isVisible());
  }
}

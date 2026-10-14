package org.bytefight.webserver.gamematch.domain.dto;

import lombok.Value;

import org.bytefight.webserver.gamematch.domain.GameOutcomeReason;

@Value
public class GameOutcomeReasonDto {
  String code;
  String displayLabel;

  public static GameOutcomeReasonDto from(GameOutcomeReason outcomeReason) {
    return new GameOutcomeReasonDto(outcomeReason.getCode(), outcomeReason.getDisplayLabel());
  }
}

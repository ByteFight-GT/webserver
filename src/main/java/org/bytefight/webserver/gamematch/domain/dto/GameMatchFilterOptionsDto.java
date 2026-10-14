package org.bytefight.webserver.gamematch.domain.dto;

import lombok.Value;

import java.util.List;

@Value
public class GameMatchFilterOptionsDto {
  List<String> mapCodes;
  List<GameOutcomeReasonDto> outcomeReasons;
  boolean hasOtherOutcomeReasons;
}

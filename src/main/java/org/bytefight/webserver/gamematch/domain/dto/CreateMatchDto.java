package org.bytefight.webserver.gamematch.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.util.Map;

@Value
public class CreateMatchDto {
  @NotNull String competitionSlug;
  @NotNull String teamAUuid;
  @NotNull String teamBUuid;
  @NotNull String ladder;
  Map<String, Object> matchSettings;
  Integer count;
}

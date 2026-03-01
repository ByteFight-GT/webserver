package org.bytefight.webserver.gamematch.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.io.Serializable;

import org.bytefight.webserver.gamematch.domain.MatchStatus;

@Value
public class GameMatchResult implements Serializable {
  @NotNull String uuid;
  @NotNull MatchStatus status;
}

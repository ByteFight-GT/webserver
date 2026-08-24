package org.bytefight.webserver.gamematch.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.io.Serializable;

import org.bytefight.webserver.gamematch.domain.MatchStatus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Value
public class GameMatchResult implements Serializable {
  @NotNull String uuid;
  @NotNull MatchStatus status;
  String mapCode;
  String outcomeReasonCode;

  @JsonCreator
  public GameMatchResult(
      @JsonProperty("uuid") String uuid,
      @JsonProperty("status") MatchStatus status,
      @JsonProperty("mapCode") String mapCode,
      @JsonProperty("outcomeReasonCode") String outcomeReasonCode) {
    this.uuid = uuid;
    this.status = status;
    this.mapCode = mapCode;
    this.outcomeReasonCode = outcomeReasonCode;
  }

  public GameMatchResult(String uuid, MatchStatus status) {
    this(uuid, status, null, null);
  }
}

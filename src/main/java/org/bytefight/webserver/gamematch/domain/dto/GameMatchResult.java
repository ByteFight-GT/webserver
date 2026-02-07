package org.bytefight.webserver.gamematch.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Value;
import org.bytefight.webserver.gamematch.domain.MatchStatus;

import java.io.Serializable;

@Value
public class GameMatchResult implements Serializable {
    @NotNull String uuid;
    @NotNull MatchStatus status;
}
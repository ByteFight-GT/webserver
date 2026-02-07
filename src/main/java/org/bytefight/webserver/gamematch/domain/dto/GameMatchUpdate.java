package org.bytefight.webserver.gamematch.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GameMatchUpdate {
    @NotNull String uuid;
    @NotNull boolean started;
}

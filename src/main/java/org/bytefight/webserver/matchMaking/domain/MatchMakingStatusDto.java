package org.bytefight.webserver.matchMaking.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class MatchMakingStatusDto {
    boolean running;
    Instant lastRunAt;
    Instant nextRunAt;
}

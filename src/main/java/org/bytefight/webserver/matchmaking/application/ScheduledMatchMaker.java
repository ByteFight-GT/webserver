package org.bytefight.webserver.matchmaking.application;

import org.bytefight.webserver.matchmaking.infra.MatchMakingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "is-prod-env", havingValue = "true")
public class ScheduledMatchMaker {
    private final MatchMakingProperties props;
    private final MatchmakingService matchMakingService;

    private final AtomicReference<Instant> lastRun = new AtomicReference<>(null);

    public Instant getLastRun() {
        return lastRun.get();
    }
}


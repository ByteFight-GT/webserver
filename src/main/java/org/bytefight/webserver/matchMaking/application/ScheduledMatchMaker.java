package org.bytefight.webserver.matchMaking.application;

import org.bytefight.webserver.matchMaking.domain.MATCHMAKING_REASON;
import org.bytefight.webserver.matchMaking.domain.MatchmakingEvent;
import org.bytefight.webserver.matchMaking.infra.MatchMakingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "is-prod-env", havingValue = "true")
public class ScheduledMatchMaker {
    private final MatchMakingProperties props;
    private final MatchMakingService matchMakingService;

    private final AtomicReference<Instant> lastRun = new AtomicReference<>(null);

    public void scheduleMatchGeneration() {
        if(matchMakingService.isEnabled()) {
            log.info("Scheduling Matches");
            MatchmakingEvent event = matchMakingService.createEvent(MATCHMAKING_REASON.SCHEDULED);
            matchMakingService.queueEvent(event);
            lastRun.set(Instant.now());
        }
    }

    public Instant getLastRun() {
        return lastRun.get();
    }
}


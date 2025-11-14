package com.example.botfightwebserver.matchMaking.application;

import com.example.botfightwebserver.matchMaking.domain.MATCHMAKING_REASON;
import com.example.botfightwebserver.matchMaking.domain.MatchMakingEvent;
import com.example.botfightwebserver.matchMaking.infra.MatchMakingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
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

    @Scheduled(cron = "${matchmaking.cron}", zone = "${matchmaking.tz:UTC}")
    public void scheduleMatchGeneration() {
        if(props.isEnabled()) {
            log.info("Scheduling Matches");
            MatchMakingEvent event = matchMakingService.createEvent(MATCHMAKING_REASON.SCHEDULED);
            matchMakingService.queueEvent(event);
            lastRun.set(Instant.now());
        }
    }

    public Instant getLastRun() {
        return lastRun.get();
    }
}


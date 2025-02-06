package com.example.botfightwebserver.matchMaking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledMatchMaker {

    private final MatchMaker matchMaker;

    @Scheduled(cron = "0 0 */1 * * *")
    public void scheduleMatchGeneration() {
        log.info("Scheduling Matches");
        matchMaker.generateMatches(true, MATCHMAKING_REASON.SCHEDULED);
    }
}


package com.example.botfightwebserver.matchMaking.application;

import com.example.botfightwebserver.matchMaking.domain.MATCHMAKING_REASON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "is-prod-env", havingValue = "true")
public class ScheduledMatchMaker {

    private final MatchMaker matchMaker;

//    public void scheduleMatchGeneration() {
//        log.info("Scheduling Matches");
//        matchMaker.generateMatches(true, MATCHMAKING_REASON.SCHEDULED);
//    }
}


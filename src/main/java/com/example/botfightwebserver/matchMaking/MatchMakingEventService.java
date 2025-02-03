package com.example.botfightwebserver.matchMaking;

import com.example.botfightwebserver.config.ClockConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MatchMakingEventService {

    private final MatchMakingEventRepository matchMakingEventRepository;
    private final ClockConfig clockConfig;

    public MatchMakingEvent createEvent(Integer numberTeams, Integer numMatches, MATCHMAKING_REASON reason) {
        MatchMakingEvent event = MatchMakingEvent.builder()
            .numberTeams(numberTeams)  // or whatever values you need
            .numberMatches(numMatches)
            .creationDateTime(LocalDateTime.now(clockConfig.clock()))
            .build();
        matchMakingEventRepository.save(event);
        return event;
    }
}

package com.example.botfightwebserver.matchMaking;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
public class MatchMakerController {

    private final MatchMaker matchMaker;
    private final MatchMakingEventService matchMakingEventService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> generateMatches() {
        matchMaker.generateMatches(false, MATCHMAKING_REASON.MANUAL);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/public/last-scheduled")
    public ResponseEntity<MatchMakingEvent>  getLastScheduledMatchMaking() {
        Optional<MatchMakingEvent> maybeLastEvent = matchMakingEventService.getLastScheduledEvent();
        return maybeLastEvent
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
package com.example.botfightwebserver.matchMaking.infra;

import com.example.botfightwebserver.matchMaking.domain.MATCHMAKING_REASON;
import com.example.botfightwebserver.matchMaking.application.MatchMakingService;
import com.example.botfightwebserver.matchMaking.application.MatchMaker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
public class MatchMakerController {
    private final MatchMakingService matchMakingService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> generateMatches() {
//        MatchMakingEvent event = matchMakingService.createEvent(MATCHMAKING_REASON.MANUAL);
//        matchMakingService.queueEvent(event);
        return ResponseEntity.ok().build();
    }

//    @GetMapping("/public/last-scheduled")
//    public ResponseEntity<MatchMakingEvent>  getLastScheduledMatchMaking() {
//        Optional<MatchMakingEvent> maybeLastEvent = matchMakingService.getLastScheduledEvent();
//        return maybeLastEvent
//            .map(ResponseEntity::ok)
//            .orElse(ResponseEntity.notFound().build());
//    }
}
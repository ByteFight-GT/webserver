package org.bytefight.webserver.matchMaking.infra;

import org.bytefight.webserver.matchMaking.application.MatchMakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
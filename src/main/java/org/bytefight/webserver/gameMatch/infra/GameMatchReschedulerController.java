package org.bytefight.webserver.gameMatch.infra;

import org.bytefight.webserver.gameMatch.application.GameMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rescheduler")
public class GameMatchReschedulerController {

    private final GameMatchService gameMatchService;

    @PostMapping("/stale")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rescheduleStaleMatches() {
        gameMatchService.rescheduleStaleMatches(true);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/reschedule/{matchId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rescheduleMatch(@PathVariable Long matchId) {
        if (!gameMatchService.isGameMatchIdExist(matchId)) {
            throw new IllegalArgumentException("Match id " + matchId + " does not exist");
        }
        gameMatchService.rescheduleMatch(matchId, true);
        return ResponseEntity.ok().build();
    }
}

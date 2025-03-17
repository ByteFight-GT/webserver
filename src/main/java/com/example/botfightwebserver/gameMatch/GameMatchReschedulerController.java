package com.example.botfightwebserver.gameMatch;

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

    @PostMapping("/stale-and-failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rescheduleStaleAndFailedMatches() {
        gameMatchService.rescheduleFailedAndStaleMatches();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stale")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rescheduleStaleMatches() {
        gameMatchService.rescheduleStaleMatches();
        return ResponseEntity.ok().build();
    }


    @PostMapping("/reschedule/{match_id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rescheduleMatch(@PathVariable Long matchId) {
        if (!gameMatchService.isGameMatchIdExist(matchId)) {
            throw new IllegalArgumentException("Match id " + matchId + " does not exist");
        }
        GameMatch match = gameMatchService.getReferenceById(matchId);
        gameMatchService.rescheduleMatch(match);
        return ResponseEntity.ok().build();
    }

}

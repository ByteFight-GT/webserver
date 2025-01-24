package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.User.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/game-match")
public class GameMatchController {

    private final GameMatchService gameMatchService;

    @PostMapping("/submit/match")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GameMatchJob> submitMatch(@RequestBody MatchSubmissionRequest request) {
        // add validation logic here for match reason
        GameMatchJob job = gameMatchService.submitGameMatch(
            request.getTeam1Id(),
            request.getTeam2Id(),
            request.getSubmission1Id(),
            request.getSubmission2Id(),
            request.getReason(),
            request.getMap()
        );
        return ResponseEntity.ok(job);
    }

    @PostMapping("/queue/remove_all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GameMatchJob>> removeAllQueuedMatches() {
        return ResponseEntity.ok(gameMatchService.deleteQueuedMatches());
    }

    @GetMapping("/queued")
    public ResponseEntity<List<GameMatchJob>> queued() {
        return ResponseEntity.ok(gameMatchService.peekQueuedMatches());
    }

    @PostMapping(value="/reschedule/all", produces = MediaType.APPLICATION_JSON_VALUE)
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GameMatchJob>> rescheduleAllQueuedMatches() {
        System.out.println("I AM HERE WATF");
        List<GameMatchJob> jobs = gameMatchService.rescheduleFailedAndStaleMatches();
        System.out.println(jobs.size());
        for (GameMatchJob job : jobs) {
            System.out.println(job);
        }
        return ResponseEntity.ok(jobs);
    }

}

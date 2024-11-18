package com.example.botfightwebserver.gameMatch;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1/game-match")
public class GameMatchController {

    private final GameMatchService gameMatchService;

    @PostMapping("/submit/match")
    public ResponseEntity<GameMatchJob> submitMatch(@RequestBody MatchSubmissionRequest request) {
        // add validation logic here for match reason
        GameMatchJob job = gameMatchService.submitGameMatch(
            request.getPlayer1Id(),
            request.getPlayer2Id(),
            request.getSubmission1Id(),
            request.getSubmission2Id(),
            request.getReason(),
            request.getMap()
        );
        return ResponseEntity.ok(job);
    }

    @PostMapping("/queue/remove_all")
    public ResponseEntity<List<GameMatchJob>> removeAllQueuedMatches() {
        return ResponseEntity.ok(gameMatchService.deleteQueuedMatches());
    }

    @GetMapping("/queued")
    public ResponseEntity<List<GameMatchJob>> queued() {
        return ResponseEntity.ok(gameMatchService.peekQueuedMatches());
    }

    @PostMapping("/reschedule/all")
    public ResponseEntity<List<GameMatchJob>> rescheduleAllQueuedMatches() {
        return ResponseEntity.ok(gameMatchService.rescheduleFailedAndStaleMatches());
    }

}

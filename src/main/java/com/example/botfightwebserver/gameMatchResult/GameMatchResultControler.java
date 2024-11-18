package com.example.botfightwebserver.gameMatchResult;

import com.example.botfightwebserver.gameMatch.GameMatchJob;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1/game-match-result")
public class GameMatchResultControler {

    private final GameMatchResultHandler gameMatchResultHandler;

    @PostMapping("/handle/results")
    public ResponseEntity<GameMatchResult> handleMatchResults(@RequestBody GameMatchResult result) {
        gameMatchResultHandler.handleGameMatchResult(result);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/submit/results")
    public ResponseEntity<GameMatchResult> submitResults(@RequestBody GameMatchResult result) {
        gameMatchResultHandler.submitGameMatchResults(result);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/queue/remove_all")
    public ResponseEntity<List<GameMatchResult>> removeAllQueuedResults() {
        return ResponseEntity.ok(gameMatchResultHandler.deleteQueuedMatches());
    }

}

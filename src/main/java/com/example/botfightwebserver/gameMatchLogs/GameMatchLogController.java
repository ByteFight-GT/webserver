package com.example.botfightwebserver.gameMatchLogs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/game-match-log")
public class GameMatchLogController {

    private final GameMatchLogService gameMatchLogService;

    @GetMapping("/all")
    public ResponseEntity<List<GameMatchLog>> getGameMatchLogs() {
        return ResponseEntity.ok(gameMatchLogService.getAllGameMatchLogs());
    }

    @GetMapping("/id")
    public ResponseEntity<GameMatchLog> getGameMatchLogsByGameId(@RequestParam Long id) {
        Optional<GameMatchLog> maybeLog = gameMatchLogService.getGameMatchLogById(id);
        return maybeLog.isPresent() ? ResponseEntity.ok(maybeLog.get()) : ResponseEntity.notFound().build();
    }

    @GetMapping("/ids")
    public ResponseEntity<List<Long>> getGameMatchIds() {
        return ResponseEntity.ok(gameMatchLogService.getGameMatchLogIds());
    }

    @GetMapping("/from-match-id")
    public ResponseEntity<GameMatchLog> getGameMatchLogFromMatchId(@RequestParam Long id) {
        Optional<GameMatchLog> maybeLog = gameMatchLogService.getMatchLogFromGame(id);
        if (maybeLog == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(maybeLog.get());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleException(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}

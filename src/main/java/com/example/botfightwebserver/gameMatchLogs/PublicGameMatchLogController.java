package com.example.botfightwebserver.gameMatchLogs;

import com.google.api.Http;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/game-match-log")
public class PublicGameMatchLogController {

    private final GameMatchLogService gameMatchLogService;

    @GetMapping("/all")
    public ResponseEntity<List<GameMatchLogDTO>> getGameMatchLogs() {
        return ResponseEntity.ok(gameMatchLogService.getAllGameMatchLogs().stream().map(GameMatchLogDTO::from).toList());
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<GameMatchLogDTO> getGameMatchLogsByGameMatchUuid(@PathVariable String uuid) {
        GameMatchLog gmLog = gameMatchLogService.getGameMatchLog(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game match log not found"));
        return ResponseEntity.ok(GameMatchLogDTO.from(gmLog));
    }

    @GetMapping("/ids")
    public ResponseEntity<List<Long>> getGameMatchIds() {
        return ResponseEntity.ok(gameMatchLogService.getGameMatchLogIds());
    }

    @GetMapping("/from-match-id")
    public ResponseEntity<GameMatchLogDTO> getGameMatchLogFromMatchId(@RequestParam Long id) {
        Optional<GameMatchLog> maybeLog = gameMatchLogService.getMatchLogFromGame(id);
        if (maybeLog == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(GameMatchLogDTO.from(maybeLog.get()));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getGameMatchLogCount() {
        return ResponseEntity.ok(gameMatchLogService.getGameMatchLogCount());
    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleException(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.toString());
    }
}

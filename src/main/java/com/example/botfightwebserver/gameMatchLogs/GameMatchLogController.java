package com.example.botfightwebserver.gameMatchLogs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1/game-match-log")
public class GameMatchLogController {

    private final GameMatchLogService gameMatchLogService;

    @GetMapping
    public ResponseEntity<List<GameMatchLog>> getGameMatchLogs() {
        return ResponseEntity.ok(gameMatchLogService.getAllGameMatchLogs());
    }
}

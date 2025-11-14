package com.example.botfightwebserver.gameMatch.infra;

import com.example.botfightwebserver.gameMatch.application.GameMatchService;
import com.example.botfightwebserver.gameMatch.domain.*;
import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.team.domain.StatsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/game-match")
public class GameMatchController {

    private final GameMatchService gameMatchService;
    private final PlayerService playerService;

    @PostMapping("/submit/match")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GameMatchDto> submitMatch(@RequestBody MatchSubmissionRequest request) {
        // add validation logic here for match reason
        GameMatch match = gameMatchService.createMatch(
                request.getTeam1Uuid(),
                request.getTeam2Uuid(),
                request.getSubmission1Uuid(),
                request.getSubmission2Uuid(),
                request.getReason(),
                request.getMap()
        );
        gameMatchService.queueMatch(match);
        return ResponseEntity.ok(GameMatchDto.fromEntity(match));
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

    @GetMapping("/public/stats")
    public ResponseEntity<StatsDTO> stats(@RequestParam Long teamId, @RequestParam MATCH_REASON reason) {
        if (teamId == null) {
            throw new IllegalArgumentException("team id can't be null");
        }
        return ResponseEntity.ok(gameMatchService.getTeamStatsByMatchReason(teamId, reason));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleException(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}

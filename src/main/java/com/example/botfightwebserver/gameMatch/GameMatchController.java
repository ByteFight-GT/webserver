package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.leaderboard.LeaderboardDTO;
import com.example.botfightwebserver.player.Player;
import com.example.botfightwebserver.player.PlayerService;
import com.example.botfightwebserver.team.StatsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/game-match")
public class GameMatchController {

    private final GameMatchService gameMatchService;
    private final PlayerService playerService;

    @PostMapping("/submit/match")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GameMatchDTO> submitMatch(@RequestBody MatchSubmissionRequest request) {
        // add validation logic here for match reason
        GameMatch match = gameMatchService.submitGameMatch(
            request.getTeam1Id(),
            request.getTeam2Id(),
            request.getSubmission1Id(),
            request.getSubmission2Id(),
            request.getReason(),
            request.getMap()
        );
        return ResponseEntity.ok(GameMatchDTO.fromEntity(match));
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

    @PostMapping("/reschedule/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GameMatchJob>> rescheduleAllQueuedMatches() {
        List<GameMatchJob> jobs = gameMatchService.rescheduleFailedAndStaleMatches(true);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/my-logs")
    public ResponseEntity<List<GameMatchDTO>> myLogs() {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Long teamId = playerService.getTeamFromUUID(UUID.fromString(authId));
        return ResponseEntity.ok(gameMatchService.getPlayedTeamMatches(teamId));
    }

    @GetMapping("/my-logs/paginated")
    public ResponseEntity<Page<GameMatchDTO>> myLogs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Player player = playerService.getPlayer(UUID.fromString(authId));
        Long teamId = player.getTeamId();
        return ResponseEntity.ok(gameMatchService.getPlayedTeamMatches(teamId, page, size));
    }

    @GetMapping("/public/logs/paginated")
    public ResponseEntity<Page<GameMatchDTO>> logs(
        @RequestParam Long teamId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) Long otherTeamId
    ) {
        if (otherTeamId != null) {
            return ResponseEntity.ok(gameMatchService.getPlayedTeamMatches(teamId, otherTeamId, page, size));
        }
        return ResponseEntity.ok(gameMatchService.getPlayedTeamMatches(teamId, page, size));
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

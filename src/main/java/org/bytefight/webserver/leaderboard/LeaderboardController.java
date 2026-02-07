package org.bytefight.webserver.leaderboard;

import io.swagger.v3.oas.annotations.Operation;
import org.bytefight.webserver.leaderboard.domain.LeaderboardDto;
import org.bytefight.webserver.team.application.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/leaderboard")
public class LeaderboardController {
    private final TeamService teamService;

    @GetMapping("/{slug}")
    @Operation(
            operationId = "getLeaderboardByCompetition",
            summary = "Get a full leaderboard by the competition slug"
    )
    public ResponseEntity<List<LeaderboardDto>> getLeaderboardByCompetition(@PathVariable String slug) {
        return ResponseEntity.ok(teamService.getLeaderboard());
    }

    @GetMapping("/all/paginated")
    public ResponseEntity<Page<LeaderboardDto>> paginateLeaderboard(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(teamService.getLeaderboard(page,size));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleException(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}


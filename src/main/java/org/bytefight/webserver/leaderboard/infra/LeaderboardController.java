package org.bytefight.webserver.leaderboard.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.bytefight.webserver.competition.application.CompetitionService;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.leaderboard.application.LeaderboardService;
import org.bytefight.webserver.leaderboard.domain.LeaderboardDto;
import org.bytefight.webserver.team.application.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Leaderboard (Public)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/leaderboard")
public class LeaderboardController {
  private final LeaderboardService leaderboardService;
  private final CompetitionService competitionService;
  private final TeamService teamService;

  @GetMapping("/{competitionSlug}/{ladderSlug}")
  @Operation(
      operationId = "getLadderLeaderboardByCompetition",
      summary = "Get a full leaderboard by the competition slug")
  public ResponseEntity<List<LeaderboardDto>> getLadderLeaderboardByCompetition(
      @PathVariable String competitionSlug, @PathVariable String ladderSlug) {
    Competition competition =
        competitionService
            .getCompetitionBySlug(competitionSlug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    return ResponseEntity.ok(
        leaderboardService.getFullLeaderboardByCompetitionAndLadder(competition, ladderSlug));
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ProblemDetail handleException(Exception e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
  }
}

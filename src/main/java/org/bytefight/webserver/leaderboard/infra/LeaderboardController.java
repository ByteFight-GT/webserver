package org.bytefight.webserver.leaderboard.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import org.bytefight.webserver.competition.application.CompetitionService;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.leaderboard.application.LeaderboardService;
import org.bytefight.webserver.leaderboard.domain.LeaderboardDto;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Leaderboard (Public)")
@RestController
@RequestMapping("/api/v1/public/leaderboard")
public class LeaderboardController {
  private final LeaderboardService leaderboardService;
  private final CompetitionService competitionService;

  public LeaderboardController(
      LeaderboardService leaderboardService, CompetitionService competitionService) {
    this.leaderboardService = leaderboardService;
    this.competitionService = competitionService;
  }

  @GetMapping("/{competitionSlug}/{ladderSlug}")
  @Operation(
      operationId = "getLadderLeaderboardByCompetition",
      summary = "Get a full leaderboard by the competition slug")
  public ResponseEntity<List<LeaderboardDto>> getLadderLeaderboardByCompetition(
      @PathVariable String competitionSlug, @PathVariable String ladderSlug) {
    return ResponseEntity.ok(
        leaderboardService.getFullLeaderboardByCompetitionAndLadder(
            requireCompetition(competitionSlug), ladderSlug));
  }

  @GetMapping("/{competitionSlug}/{ladderSlug}/page")
  @Operation(
      operationId = "getLadderLeaderboardPage",
      summary =
          "Search a leaderboard page. Page size is the competition's LEADERBOARD_PAGINATION size.")
  public Page<LeaderboardDto> getLadderLeaderboardPage(
      @PathVariable String competitionSlug,
      @PathVariable String ladderSlug,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(required = false) String search) {
    return leaderboardService.searchLeaderboard(
        requireCompetition(competitionSlug), ladderSlug, search, page);
  }

  private Competition requireCompetition(String competitionSlug) {
    return competitionService
        .getCompetitionBySlug(competitionSlug)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ProblemDetail handleStatus(ResponseStatusException exception) {
    HttpStatusCode status = exception.getStatusCode();
    String detail = exception.getReason() == null ? exception.getMessage() : exception.getReason();
    return ProblemDetail.forStatusAndDetail(status, detail);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ProblemDetail handleException(Exception e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
  }
}

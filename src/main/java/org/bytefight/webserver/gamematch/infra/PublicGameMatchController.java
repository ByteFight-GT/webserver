package org.bytefight.webserver.gamematch.infra;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.bytefight.webserver.competition.application.CompetitionService;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.application.GameMatchFilterOptionsService;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchDto;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchFilterOptionsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Game Match (Public)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/game-match")
public class PublicGameMatchController {
  private final GameMatchService gameMatchService;
  private final GameMatchFilterOptionsService gameMatchFilterOptionsService;
  private final CompetitionService competitionService;

  @GetMapping("/{uuid}")
  public ResponseEntity<GameMatchDto> getGameMatchByUuid(@PathVariable String uuid) {
    return ResponseEntity.ok(
        gameMatchService
            .getDTOByUuid(uuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }

  @GetMapping
  public ResponseEntity<Page<GameMatchDto>> searchGameMatches(
      @RequestParam String competitionSlug,
      @RequestParam(required = false) String teamUuid,
      @RequestParam(required = false) String opponentTeamName,
      @RequestParam(defaultValue = "Any") String teamWin,
      @RequestParam(required = false) String initiatingTeamUuid,
      @RequestParam(required = false) String notInitiatingTeamUuid,
      @RequestParam(required = false) String submissionUuid,
      @RequestParam(required = false) String ladder,
      @RequestParam(required = false) MatchReason matchReason,
      @RequestParam(required = false) MatchStatus matchStatus,
      @RequestParam(required = false) String mapCode,
      @RequestParam(required = false) String outcomeReasonCode,
      @RequestParam(required = false) Boolean unregisteredOutcomeReason,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    PageRequest pageRequest =
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt"));
    Competition competition =
        competitionService
            .getCompetitionBySlug(competitionSlug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Page<GameMatch> gameMatches =
        gameMatchService.getPaginatedMatches(
            competition,
            ladder,
            teamUuid,
            opponentTeamName,
            teamWin,
            initiatingTeamUuid,
            notInitiatingTeamUuid,
            submissionUuid,
            matchReason,
            matchStatus,
            mapCode,
            outcomeReasonCode,
            unregisteredOutcomeReason,
            pageRequest);

    return ResponseEntity.ok(gameMatches.map(GameMatchDto::fromEntity));
  }

  @GetMapping("/filter-options")
  public ResponseEntity<GameMatchFilterOptionsDto> getFilterOptions(
      @RequestParam String competitionSlug) {
    Competition competition =
        competitionService
            .getCompetitionBySlug(competitionSlug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return ResponseEntity.ok(gameMatchFilterOptionsService.getFilterOptions(competition));
  }

  @GetMapping("/queue/{competitionSlug}")
  public ResponseEntity<Page<GameMatchDto>> getCompetitionGameMatchQueue(
      @PathVariable String competitionSlug,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    PageRequest pageRequest =
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt"));
    Competition competition =
        competitionService
            .getCompetitionBySlug(competitionSlug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Page<GameMatch> gameMatches = gameMatchService.getFullPaginatedQueue(competition, pageRequest);

    return ResponseEntity.ok(gameMatches.map(GameMatchDto::fromEntity));
  }
}

package org.bytefight.webserver.ladder.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.bytefight.webserver.competition.application.CompetitionService;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.ladder.application.LadderService;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.ladder.domain.dto.PublicLadderDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Ladder (Public)")
@RequestMapping({"/api/v1/public/ladder"})
@RestController
@RequiredArgsConstructor
public class PublicLadderController {
  private final LadderService ladderService;
  private final CompetitionService competitionService;

  @GetMapping("/{competitionSlug}")
  @Operation(
      operationId = "getLaddersByCompetition",
      summary = "List all ladders  for a competition")
  public ResponseEntity<List<PublicLadderDto>> getLaddersByCompetition(
      @PathVariable String competitionSlug) {
    Competition competition =
        competitionService
            .getCompetitionBySlug(competitionSlug)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found"));
    List<Ladder> ladders = ladderService.getLaddersByCompetition(competition);

    return ResponseEntity.ok(PublicLadderDto.listFrom(ladders));
  }

  @GetMapping("/{competitionSlug}/{ladderSlug}")
  @Operation(
      operationId = "getLaddersByCompetition",
      summary = "List all ladders  for a competition")
  public ResponseEntity<PublicLadderDto> getLaddersByCompetition(
      @PathVariable String competitionSlug, @PathVariable String ladderSlug) {
    Competition competition =
        competitionService
            .getCompetitionBySlug(competitionSlug)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found"));
    Ladder ladder =
        ladderService
            .getLadder(competition, ladderSlug)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ladder not found"));

    return ResponseEntity.ok(PublicLadderDto.from(ladder));
  }
}

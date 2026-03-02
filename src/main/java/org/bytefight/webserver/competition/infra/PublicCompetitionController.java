package org.bytefight.webserver.competition.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import org.bytefight.webserver.competition.application.CompetitionService;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.domain.dto.CompetitionDto;
import org.bytefight.webserver.team.application.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(
    name = "Competitions (Public)",
    description =
        """
        Public, read-only endpoints for browsing competitions and their related data,
        including teams, standings, and basic metadata.
        """)
@RestController
@RequestMapping("/api/v1/public/competition")
@RequiredArgsConstructor
public class PublicCompetitionController {
  private final CompetitionService competitionService;
  private final TeamService teamService;

  @GetMapping("/{slug}")
  @Operation(operationId = "getCompetitionBySlug", summary = "Get a public competition by slug")
  public ResponseEntity<CompetitionDto> getCompetition(@PathVariable String slug) {
    Competition competition =
        competitionService
            .getCompetitionBySlug(slug)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Competition with slug not found"));
    return ResponseEntity.ok(CompetitionDto.from(competition));
  }

  @GetMapping("/")
  @Operation(operationId = "getAllCompetitions", summary = "Return all competitions")
  public ResponseEntity<List<CompetitionDto>> getAllCompetitions() {
    return ResponseEntity.ok(
        competitionService.getAllCompetitions().stream()
            .map(CompetitionDto::from)
            .collect(Collectors.toList()));
  }
}

package org.bytefight.webserver.gamematch.infra;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.bytefight.webserver.competition.application.CompetitionService;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.application.GameOutcomeReasonService;
import org.bytefight.webserver.gamematch.domain.dto.GameOutcomeReasonManifestDto;
import org.bytefight.webserver.gamematch.domain.dto.GameOutcomeReasonManifestEntry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Game Outcome Reason Manifest")
@PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_ACCOUNT')")
@RequestMapping("/api/v1/internal/competition/{competitionSlug}/game-outcome-reason-manifest")
@RequiredArgsConstructor
@RestController
public class GameOutcomeReasonManifestController {
  private final CompetitionService competitionService;
  private final GameOutcomeReasonService gameOutcomeReasonService;

  @PostMapping
  public ResponseEntity<Void> registerManifest(
      @PathVariable String competitionSlug,
      @Valid @RequestBody GameOutcomeReasonManifestDto manifest) {
    Competition competition =
        competitionService
            .getCompetitionBySlug(competitionSlug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    gameOutcomeReasonService.registerManifest(
        competition,
        manifest.reasons().stream()
            .map(entry -> new GameOutcomeReasonManifestEntry(entry.code(), entry.defaultLabel()))
            .toList());
    return ResponseEntity.noContent().build();
  }
}

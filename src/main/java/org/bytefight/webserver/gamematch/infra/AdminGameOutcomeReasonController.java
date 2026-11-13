package org.bytefight.webserver.gamematch.infra;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.bytefight.webserver.competition.application.AdminCompetitionService;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.application.GameOutcomeReasonService;
import org.bytefight.webserver.gamematch.domain.dto.AdminCreateGameOutcomeReasonDto;
import org.bytefight.webserver.gamematch.domain.dto.AdminGameOutcomeReasonDto;
import org.bytefight.webserver.gamematch.domain.dto.AdminUpdateGameOutcomeReasonDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Game Outcome Reason (Admin)")
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/game-outcome-reason")
@RequiredArgsConstructor
@RestController
public class AdminGameOutcomeReasonController {
  private final AdminCompetitionService adminCompetitionService;
  private final GameOutcomeReasonService gameOutcomeReasonService;

  @GetMapping
  public List<AdminGameOutcomeReasonDto> listReasons(@RequestParam Long competitionId) {
    adminCompetitionService.getCompetition(competitionId);
    return gameOutcomeReasonService.listReasons(competitionId).stream()
        .map(AdminGameOutcomeReasonDto::from)
        .toList();
  }

  @PostMapping
  public ResponseEntity<AdminGameOutcomeReasonDto> createReason(
      @Valid @RequestBody AdminCreateGameOutcomeReasonDto input) {
    Competition competition = adminCompetitionService.getCompetition(input.competitionId());
    AdminGameOutcomeReasonDto response =
        AdminGameOutcomeReasonDto.from(
            gameOutcomeReasonService.createReason(
                competition, input.code(), input.displayLabel()));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PatchMapping("/{id}")
  public AdminGameOutcomeReasonDto updateReason(
      @PathVariable Long id, @Valid @RequestBody AdminUpdateGameOutcomeReasonDto input) {
    return AdminGameOutcomeReasonDto.from(
        gameOutcomeReasonService.updateReasonConfiguration(
            id, input.displayLabel(), input.visible()));
  }
}

package org.bytefight.webserver.leaderboard.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.bytefight.webserver.competition.application.AdminCompetitionService;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.leaderboard.domain.AdminUpdateLeaderboardSettingsDto;
import org.bytefight.webserver.leaderboard.domain.LeaderboardSettingsDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Leaderboard (Admin)")
@RequestMapping("/api/v1/admin/leaderboard")
@PreAuthorize("hasRole('ADMIN')")
@RestController
public class AdminLeaderboardController {
  private final AdminCompetitionService adminCompetitionService;

  public AdminLeaderboardController(AdminCompetitionService adminCompetitionService) {
    this.adminCompetitionService = adminCompetitionService;
  }

  @GetMapping("/{competitionId}")
  @Operation(
      operationId = "adminGetLeaderboardSettings",
      summary = "Get LEADERBOARD_PAGINATION for a competition")
  public LeaderboardSettingsDto getSettings(@PathVariable Long competitionId) {
    Competition competition = adminCompetitionService.getCompetition(competitionId);
    return LeaderboardSettingsDto.from(competition);
  }

  @PatchMapping("/{competitionId}")
  @Operation(
      operationId = "adminUpdateLeaderboardSettings",
      summary = "Update LEADERBOARD_PAGINATION and rows per page for a competition")
  public LeaderboardSettingsDto updateSettings(
      @PathVariable Long competitionId,
      @Valid @RequestBody AdminUpdateLeaderboardSettingsDto input) {
    Competition competition =
        adminCompetitionService.updateLeaderboardSettings(
            competitionId, input.getLeaderboardPagination(), input.getLeaderboardPageSize());
    return LeaderboardSettingsDto.from(competition);
  }
}

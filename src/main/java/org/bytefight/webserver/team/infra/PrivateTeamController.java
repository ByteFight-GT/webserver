package org.bytefight.webserver.team.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.util.*;

import org.bytefight.webserver.config.ClockConfig;
import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.submission.application.SubmissionService;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.dto.SelfTeamDto;
import org.bytefight.webserver.team.domain.dto.SetSubmissionDto;
import org.bytefight.webserver.team.domain.dto.TeamSettingsDto;
import org.bytefight.webserver.user.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Team (Private)")
@RestController
@RequestMapping("/api/v1/team")
@RequiredArgsConstructor
@Transactional
public class PrivateTeamController {
  private final TeamService teamService;
  private final PlayerService playerService;
  private final SubmissionService submissionService;
  private final Clock clock;
  private final ClockConfig clockConfig;

  @PostMapping("/{uuid}")
  @Operation(operationId = "editTeam", summary = "Update a team's settings")
  public ResponseEntity<SelfTeamDto> editTeam(
      @AuthenticationPrincipal User user,
      @PathVariable UUID uuid,
      @RequestBody TeamSettingsDto teamSettingsDto) {
    Player player =
        playerService
            .getPlayer(user)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

    Team team =
        teamService
            .getTeamByUuid(uuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));

    if (!teamService.isMember(team, player)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this team");
    }

    teamService.editTeam(team, teamSettingsDto);

    List<Player> members = teamService.getPlayersForTeam(team);
    return ResponseEntity.ok(SelfTeamDto.from(team, members));
  }

  @PatchMapping("/{uuid}/current-submission")
  @Operation(summary = "Set the team's current submission")
  public ResponseEntity<SelfTeamDto> setCurrentSubmission(
      @AuthenticationPrincipal User user,
      @PathVariable UUID uuid,
      @RequestBody SetSubmissionDto setSubmissionDto) {
    Player player =
        playerService
            .getPlayer(user)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

    Team team =
        teamService
            .getTeamByUuid(uuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));

    if (!teamService.isMember(team, player)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this team");
    }

    Submission submission =
        submissionService
            .getSubmission(UUID.fromString(setSubmissionDto.getSubmissionUuid()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

    teamService.setTeamCurrentSubmission(team, submission);

    return ResponseEntity.ok().build();
  }
}

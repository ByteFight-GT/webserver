package org.bytefight.webserver.submission.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.common.domain.PermissionDeniedException;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.DefaultLadders;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.submission.application.SubmissionService;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.dto.SubmissionDto;
import org.bytefight.webserver.submission.domain.dto.SubmissionStatusDto;
import org.bytefight.webserver.submission.domain.dto.UploadSubmissionDto;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.turnstile.infra.RequireTurnstile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/submission")
@Tag(name = "Submissions", description = "Upload submissions and access submission files")
public class SubmissionController {
  private final SubmissionService submissionService;
  private final GameMatchService gameMatchService;
  private final TeamService teamService;
  private final PlayerService playerService;

  @GetMapping("/team/{teamUuid}")
  @Operation(summary = "List all submissions for a team")
  public ResponseEntity<List<SubmissionDto>> getAllSubmissionsByTeam(
      @AuthenticationPrincipal User user, @PathVariable UUID teamUuid) {
    Player player =
        playerService
            .getPlayer(user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Team team =
        teamService
            .getTeamByUuid(teamUuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!teamService.isMember(team, player)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this team");
    }

    return ResponseEntity.ok(
        submissionService.listSubmissionsByTeam(team).stream().map(SubmissionDto::from).toList());
  }

  @GetMapping("/team/{teamUuid}/status")
  @Operation(summary = "Get status of your current submissions (e.g. amount of storage space used)")
  public ResponseEntity<SubmissionStatusDto> getTeamSubmissionStatus(
      @AuthenticationPrincipal User user, @PathVariable UUID teamUuid) {
    Player player =
        playerService
            .getPlayer(user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Team team =
        teamService
            .getTeamByUuid(teamUuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!teamService.isMember(team, player)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this team");
    }

    SubmissionStatusDto.SubmissionStatusDtoBuilder builder = SubmissionStatusDto.builder();

    builder.totalStorageSize(team.getCompetition().getTeamSubmissionStorageSize());
    builder.usedStorageSize(submissionService.getTeamSubmissionStorageSize(team));

    return ResponseEntity.ok(builder.build());
  }

  @PostMapping(path = "/team/{teamUuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(operationId = "uploadSubmission", summary = "Upload a submission for a team")
  @RequireTurnstile
  @Transactional
  public ResponseEntity<SubmissionDto> uploadSubmission(
      @AuthenticationPrincipal User user,
      @PathVariable UUID teamUuid,
      UploadSubmissionDto uploadSubmissionDto) {
    Player player =
        playerService
            .getPlayer(user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Team team =
        teamService
            .getTeamByUuid(teamUuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!teamService.isMember(team, player)) {
      log.debug("Upload denied - not team member: teamId={}, userId={}", teamUuid, user.getUuid());
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this team");
    }

    if (!team.getCompetition().isAllowNewSubmission()) {
      log.debug(
          "Upload denied - submissions closed: teamId={}, competitionId={}",
          teamUuid,
          team.getCompetition().getSlug());
      throw new PermissionDeniedException(
          "You are not allowed to create a new submission at this time");
    }

    Submission submission = null;

    try {
      submission =
          submissionService.createSubmission(
              team,
              uploadSubmissionDto.getDescription(),
              uploadSubmissionDto.getFile(),
              uploadSubmissionDto.getIsAutoSet());
    } catch (IOException e) {
      log.error(
          "Failed to upload submission: teamId={}, userId={}, error={}",
          teamUuid,
          user.getUuid(),
          e.getMessage(),
          e);
      return ResponseEntity.internalServerError().build();
    }

    GameMatch validation =
        gameMatchService.createMatch(
            user,
            team,
            team,
            submission,
            submission,
            DefaultLadders.VALIDATION,
            MatchReason.validation,
            null,
            null);

    gameMatchService.scheduleMatch(validation);

    log.info(
        "Submission uploaded: submissionId={}, teamId={}, userId={}",
        submission.getUuid(),
        teamUuid,
        user.getUuid());
    return ResponseEntity.ok(SubmissionDto.from(submission));
  }

  @DeleteMapping(path = "/team/{teamUuid}/{submissionUuid}")
  @Operation(operationId = "deleteSubmission", summary = "Deletes a team's submission")
  public ResponseEntity<Void> deleteSubmission(
      @AuthenticationPrincipal User user,
      @PathVariable UUID teamUuid,
      @PathVariable UUID submissionUuid) {
    Player player =
        playerService
            .getPlayer(user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Team team =
        teamService
            .getTeamByUuid(teamUuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!teamService.isMember(team, player)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this team");
    }

    Submission submission =
        submissionService
            .getSubmissionByTeamAndUuid(team, submissionUuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    log.info(
        "User deleting submission: submissionId={}, teamId={}, userId={}",
        submissionUuid,
        teamUuid,
        user.getUuid());
    submissionService.deleteSubmission(submission);

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/download/{uuid}")
  @Operation(
      operationId = "getSubmissionDownloadUrl",
      summary = "Get a signed download URL for a submission")
  public ResponseEntity<DownloadLinkDto> getSubmissionDownloadUrl(
      @AuthenticationPrincipal User user, @PathVariable UUID uuid) {
    return ResponseEntity.ok(submissionService.getSubmissionDownloadUri(uuid, user));
  }
}

package org.bytefight.webserver.submission.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.storage.application.LocalStorageService;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.SubmissionValidity;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SubmissionService {
  private final PlayerService playerService;
  private final SubmissionRepository submissionRepository;
  private final LocalStorageService storageService;
  private final TeamRepository teamRepository;
  private final TeamService teamService;

  public Submission createSubmission(
      Team team, String description, MultipartFile file, Boolean isAutoSet) throws IOException {
    validateFile(file);
    long storageLimit = team.getCompetition().getTeamSubmissionStorageSize();

    Long currentUsed = submissionRepository.sumUndeletedSubmissionSizeByTeam(team);
    long currentUsedSize = currentUsed != null ? currentUsed : 0L;
    if (currentUsedSize + file.getSize() > storageLimit) {
      log.warn(
          "Storage limit exceeded: teamId={}, used={}, uploading={}, limit={}",
          team.getUuid(),
          currentUsedSize,
          file.getSize(),
          storageLimit);
      throw new IllegalArgumentException(
          String.format(
              "Team storage limit exceeded: used=%d, uploading=%d, limit=%d",
              currentUsedSize, file.getSize(), storageLimit));
    }
    FileRecord storedFile =
        storageService.store(
            file, "submissions/" + team.getUuid() + "/", file.getOriginalFilename(), false);

    Submission submission = new Submission();
    submission.setUuid(storedFile.getUuid());
    submission.setFileRecord(storedFile);
    submission.setTeam(team);
    submission.setDescription(description);

    if (isAutoSet) {
      submission.setValidity(SubmissionValidity.not_evaluated_autoset);
    } else {
      submission.setValidity(SubmissionValidity.not_evaluated);
    }

    Submission saved = submissionRepository.save(submission);
    log.info(
        "Submission created: submissionId={}, teamId={}, size={}, autoSet={}",
        saved.getUuid(),
        team.getUuid(),
        file.getSize(),
        isAutoSet);
    return saved;
  }

  public Optional<Submission> getSubmissionByTeamAndUuid(Team team, UUID uuid) {
    return submissionRepository.findSubmissionByTeamAndUuidAndIsDeletedIsFalse(team, uuid);
  }

  @Transactional
  public void deleteSubmission(Submission submission) {
    if (submission.equals(submission.getTeam().getCurrentSubmission())) {
      log.warn(
          "Attempted to delete active submission: submissionId={}, teamId={}",
          submission.getUuid(),
          submission.getTeam().getUuid());
      throw new IllegalArgumentException("You can't delete your active submission");
    }

    submission.softDelete();
    submissionRepository.save(submission);

    storageService.delete(submission.getFileRecord().getUuid().toString());
    log.info(
        "Submission deleted: submissionId={}, teamId={}",
        submission.getUuid(),
        submission.getTeam().getUuid());
  }

  @Transactional
  public void onSubmissionValidationComplete(Submission submission, boolean isValid) {
    if (isValid) {
      boolean wasAutoSet = submission.getValidity() == SubmissionValidity.not_evaluated_autoset;
      if (wasAutoSet) {
        submission.getTeam().setCurrentSubmission(submission);
        teamRepository.save(submission.getTeam());
      }

      submission.setValidity(SubmissionValidity.valid);
      submissionRepository.save(submission);
      log.info(
          "Submission validated: submissionId={}, teamId={}, valid=true, autoSet={}",
          submission.getUuid(),
          submission.getTeam().getUuid(),
          wasAutoSet);
    } else {
      submission.setValidity(SubmissionValidity.invalid);
      submissionRepository.save(submission);
      log.warn(
          "Submission validation failed: submissionId={}, teamId={}",
          submission.getUuid(),
          submission.getTeam().getUuid());
    }
  }

  public Optional<Submission> getSubmission(UUID uuid) {
    return submissionRepository.findSubmissionByUuidAndIsDeletedIsFalse(uuid);
  }

  public List<Submission> listSubmissionsByTeam(Team team) {
    return submissionRepository.findSubmissionsByTeamAndIsDeletedIsFalseOrderByCreatedAtDesc(team);
  }

  public long getTeamSubmissionStorageSize(Team team) {
    if (team == null) {
      throw new IllegalArgumentException("Team is required");
    }
    Long total = submissionRepository.sumUndeletedSubmissionSizeByTeam(team);
    return total != null ? total : 0L;
  }

  public DownloadLinkDto getSubmissionDownloadUri(UUID uuid, User user) {
    Submission submission =
        submissionRepository
            .findSubmissionByUuidAndIsDeletedIsFalse(uuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    // if user is NOT an admin, we check that they own the submission
    if (!user.isAdmin()) {
      Player player =
          playerService
              .getPlayer(user)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

      if (!teamService.isMember(submission.getTeam(), player)) {
        log.warn(
            "Forbidden download attempt: submissionId={}, userId={}",
            submission.getUuid(),
            user.getUuid());
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
      }
    }

    DownloadLinkDto downloadLink =
        storageService.getDownloadLink(
            submission.getFileRecord().getUuid().toString(), Duration.ofMinutes(5));
    log.info(
        "Submission download link generated: submissionId={}, userId={}",
        submission.getUuid(),
        user.getUuid());
    return downloadLink;
  }

  public void validateFile(MultipartFile file) {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File is empty");
    }

    String contentType = file.getContentType();
  }
}

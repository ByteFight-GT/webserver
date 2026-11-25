package org.bytefight.webserver.submission.application;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;

import org.bytefight.webserver.storage.application.LocalStorageService;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.dto.AdminCreateSubmissionDto;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class AdminSubmissionService {
  private final SubmissionRepository submissionRepository;
  private final TeamRepository teamRepository;
  private final SubmissionService submissionService;
  private final LocalStorageService storageService;

  public AdminSubmissionService(
      SubmissionRepository submissionRepository,
      TeamRepository teamRepository,
      SubmissionService submissionService,
      LocalStorageService storageService) {
    this.submissionRepository = submissionRepository;
    this.teamRepository = teamRepository;
    this.submissionService = submissionService;
    this.storageService = storageService;
  }

  /** Looks up a submission by id, including soft-deleted ones. */
  public Submission getSubmission(Long id) {
    return submissionRepository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));
  }

  /**
   * Returns a short-lived signed link to a submission's file. Deleting a submission deletes its
   * file, so deleted submissions have nothing to download.
   */
  public DownloadLinkDto getDownloadLink(Long id) {
    Submission submission = getSubmission(id);
    if (submission.isDeleted()) {
      throw new ResponseStatusException(
          HttpStatus.GONE, "Submission was deleted, so its file no longer exists");
    }
    String fileUuid = submission.getFileRecord().getUuid().toString();
    if (storageService.stat(fileUuid).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission file not found");
    }
    DownloadLinkDto link = storageService.getDownloadLink(fileUuid, Duration.ofMinutes(5));
    log.info("Admin submission download link generated: submissionId={}", submission.getUuid());
    return link;
  }

  public Page<Submission> listSubmissions(boolean isDeleted, Pageable pageable) {
    return submissionRepository.findByIsDeleted(isDeleted, pageable);
  }

  public Submission createSubmission(AdminCreateSubmissionDto input) throws IOException {
    Team team =
        teamRepository
            .findById(input.getTeamId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
    Submission submission =
        submissionService.createSubmission(team, input.getDescription(), input.getFile(), false);
    if (input.getValidity() != null) {
      submission.setValidity(input.getValidity());
      submission = submissionRepository.save(submission);
    }
    log.info(
        "Admin created submission: submissionId={}, teamId={}, validity={}",
        submission.getUuid(),
        team.getUuid(),
        submission.getValidity());
    return submission;
  }
}

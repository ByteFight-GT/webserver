package com.example.botfightwebserver.submission.application;

import com.example.botfightwebserver.permissions.PermissionsService;
import com.example.botfightwebserver.storage.application.LocalStorageService;
import com.example.botfightwebserver.storage.domain.DownloadLinkDto;
import com.example.botfightwebserver.storage.domain.StoredObject;
import com.example.botfightwebserver.submission.domain.*;
import com.example.botfightwebserver.submission.infra.SubmissionRepository;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.infra.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final LocalStorageService storageService;
    private final TeamRepository teamRepository;
    private final PermissionsService permissionsService;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    public Submission createSubmission(String teamUuid, MultipartFile file, Boolean isAutoSet) throws IOException {
        validateFile(file);

        Team team = teamRepository.findByUuid(UUID.fromString(teamUuid))
            .orElseThrow(() -> new EntityNotFoundException("Team not found with uuid: " + teamUuid));

        StoredObject storedObject = storageService.store(file, "submissions/" + team.getUuid() + "/", file.getOriginalFilename());

        Submission submission = new Submission();
        submission.setStorageFileUuid(storedObject.getUuid());
        submission.setSubmissionValidity(SUBMISSION_VALIDITY.NOT_EVALUATED);
        submission.setSource(STORAGE_SOURCE.LOCAL);
        submission.setTeam(team);
        submission.setName(file.getOriginalFilename());
        submission.setIsAutoSet(isAutoSet);

        return submissionRepository.save(submission);
    }

    public Submission getSubmissionReferenceById(Long id) {
        return submissionRepository.getReferenceById(id);
    }

    public Submission deleteSubmission(Long submissionId, Long teamId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found with id: " + submissionId));

        if (!submission.getTeam().getId().equals(teamId)) {
            throw new IllegalArgumentException("You do not own this submission, so it cannot be deleted.");
        }

        submission.setIsDeleted(true);

        submissionRepository.save(submission);

        return submission;
    }

    public DownloadLinkDto getSubmissionDownloadUri(Long submissionId, Long teamId) {
        Submission submission = submissionRepository.findById(submissionId).orElseThrow();

        if (!submission.getTeam().getId().equals(teamId)) {
            throw new IllegalArgumentException("You do not own this submission, so it cannot be deleted.");
        }

        return storageService.getDownloadLink(submission.getStorageFileUuid().toString(), Duration.ofMinutes(5));
    }

    public void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize()  > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File is too large");
        }

        String contentType = file.getContentType();
    }

    public void validateSubmissions(Long submission1Id, Long submission2Id) {
        if(!submissionRepository.existsById(submission1Id) || !submissionRepository.existsById(submission2Id)) {
            throw new IllegalArgumentException("Submission 1 or 2 does not exist");
        }
    }

    public void validateSubmissionAfterMatch(long submissionId) {
        Submission submission = submissionRepository.findById(submissionId).get();
        submission.setSubmissionValidity(SUBMISSION_VALIDITY.VALID);
        submissionRepository.save(submission);
    }

    public void invalidateSubmissionAfterMatch(long submissionId) {
        Submission submission = submissionRepository.findById(submissionId).get();
        submission.setSubmissionValidity(SUBMISSION_VALIDITY.INVALID);
        submissionRepository.save(submission);
    }

    public boolean isSubmissionValid(Long submissionId) {
        Optional<Submission> maybeSubmission = submissionRepository.findById(submissionId);
        if (maybeSubmission.isPresent()) {
            return maybeSubmission.get().getSubmissionValidity() == SUBMISSION_VALIDITY.VALID;
        }
        return false;
    }

    public List<SubmissionDTO> getTeamSubmissions(Long teamId) {
        List<Submission> submissions =submissionRepository.findSubmissionsByTeamIdOrderByCreatedAtDesc(teamId);
        return submissions.stream().filter((a) -> !a.getIsDeleted()).map(SubmissionDTO::fromEntity).toList();
    }

}

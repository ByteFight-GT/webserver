package com.example.botfightwebserver.submission;

import com.example.botfightwebserver.permissions.PermissionsService;
import com.example.botfightwebserver.team.Team;
import com.example.botfightwebserver.team.TeamRepository;
import com.example.botfightwebserver.storage.StorageService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final StorageService storageService;
    private final TeamRepository teamRepository;
    private final PermissionsService permissionsService;

    public SubmissionService(SubmissionRepository submissionRepository, @Qualifier("gcpStorageServiceImpl") StorageService storageService,
                             TeamRepository teamRepository, PermissionsService permissionsService) {
        this.submissionRepository = submissionRepository;
        this.storageService = storageService;
        this.teamRepository = teamRepository;
        this.permissionsService = permissionsService;
    }

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    public Submission createSubmission(Long teamId, MultipartFile file, Boolean isAutoSet) {
        validateFile(file);

        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + teamId));

        String filePathString = storageService.uploadFile(teamId, file);

        Submission submission = new Submission();
        submission.setStoragePath(filePathString);
        submission.setSubmissionValidity(SUBMISSION_VALIDITY.NOT_EVALUATED);
        submission.setSource(STORAGE_SOURCE.GCP);
        submission.setTeamId(teamId);
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

        if (!submission.getTeamId().equals(teamId)) {
            throw new IllegalArgumentException("You do not own this submission, so it cannot be deleted.");
        }

        submission.setIsDeleted(true);

        submissionRepository.save(submission);

        return submission;
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
        return submissions.stream().map(SubmissionDTO::fromEntity).toList();
    }

}

package com.example.botfightwebserver.submission.application;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.permissions.application.PermissionsService;
import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.player.domain.Player;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SubmissionService {
    private final PlayerService playerService;
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
//        submission.setStorageFileUuid(storedObject.getUuid());
//        submission.setSubmissionValidity(SubmissionValidity.NOT_EVALUATED);
//        submission.setSource(STORAGE_SOURCE.LOCAL);
//        submission.setTeam(team);
//        submission.setName(file.getOriginalFilename());
//        submission.setIsAutoSet(isAutoSet);

        return submissionRepository.save(submission);
    }

    public Submission getSubmissionReferenceById(Long id) {
        return submissionRepository.getReferenceById(id);
    }

    public Submission getSubmissionByUuid(String uuid) {
        return submissionRepository.findSubmissionByUuid(UUID.fromString(uuid)).orElseThrow();
    }

    public Submission deleteSubmission(String submissionUuid, Long teamId) {
        Submission submission = submissionRepository.findSubmissionByUuid(UUID.fromString(submissionUuid))
                .orElseThrow(() -> new EntityNotFoundException("Submission not found with uuid: " + submissionUuid));

        if (!submission.getTeam().getId().equals(teamId)) {
            throw new IllegalArgumentException("You do not own this submission, so it cannot be deleted.");
        }

//        submission.setIsDeleted(true);

        submissionRepository.save(submission);

        return submission;
    }

    public DownloadLinkDto getSubmissionDownloadUri(String submissionUuid, User user) {
        Submission submission = submissionRepository.findSubmissionByUuid(UUID.fromString(submissionUuid)).orElseThrow();

        // if user is NOT an admin, we check that they own the submission
//        if(!user.isAdmin()) {
//            Player player = playerService.getPlayer(user);
//            Team team = player.getTeam();
//
//            if (team == null) {
//                throw new AccessDeniedException("You are not allowed to access this submission");
//            }
//
//            if (!submission.getTeam().getId().equals(team.getId())) {
//                throw new AccessDeniedException("You are not allowed to access this submission");
//            }
//        }

//        return storageService.getDownloadLink(submission.getStorageFileUuid().toString(), Duration.ofMinutes(5));
        return null;
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

    public void validateSubmissions(String submission1Uuid, String submission2Uuid) {
        if(!submissionRepository.existsByUuid(UUID.fromString(submission1Uuid)) || !submissionRepository.existsByUuid(UUID.fromString(submission2Uuid))) {
            throw new IllegalArgumentException("Submission 1 or 2 does not exist");
        }
    }

    public void validateSubmissionAfterMatch(long submissionId) {
        Submission submission = submissionRepository.findById(submissionId).get();
//        submission.setSubmissionValidity(SubmissionValidity.VALID);
        submissionRepository.save(submission);
    }

    public void invalidateSubmissionAfterMatch(long submissionId) {
        Submission submission = submissionRepository.findById(submissionId).get();
//        submission.setSubmissionValidity(SubmissionValidity.INVALID);
        submissionRepository.save(submission);
    }

    public boolean isSubmissionValid(String submissionUuid) {
        Optional<Submission> maybeSubmission = submissionRepository.findSubmissionByUuid(UUID.fromString(submissionUuid));
        if (maybeSubmission.isPresent()) {
//            return maybeSubmission.get().getSubmissionValidity() == SubmissionValidity.VALID;
        }
        return false;
    }

    public List<SubmissionDTO> getTeamSubmissions(Long teamId) {
        List<Submission> submissions =submissionRepository.findSubmissionsByTeamIdOrderByCreatedAtDesc(teamId);
//        return submissions.stream().filter((a) -> !a.getIsDeleted()).map(SubmissionDTO::from).toList();
        return null;
    }

}

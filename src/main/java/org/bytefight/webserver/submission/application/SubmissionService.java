package org.bytefight.webserver.submission.application;

import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.storage.application.LocalStorageService;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.SubmissionValidity;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.infra.TeamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    public Submission createSubmission(Team team, String description, MultipartFile file, Boolean isAutoSet) throws IOException {
        validateFile(file);
        FileRecord storedFile = storageService.store(file, "submissions/" + team.getUuid() + "/", file.getOriginalFilename());

        Submission submission = new Submission();
        submission.setUuid(storedFile.getUuid());
        submission.setFileRecord(storedFile);
        submission.setTeam(team);
        submission.setDescription(description);

        if(isAutoSet) {
            submission.setValidity(SubmissionValidity.not_evaluated_autoset);
        } else {
            submission.setValidity(SubmissionValidity.not_evaluated);
        }

        return submissionRepository.save(submission);
    }

    public Optional<Submission> getSubmissionByTeamAndUuid(Team team, UUID uuid) {
        return submissionRepository.findSubmissionByTeamAndUuidAndIsDeletedIsFalse(team, uuid);
    }

    @Transactional
    public void deleteSubmission(Submission submission) {
        if(submission.equals(submission.getTeam().getCurrentSubmission())) {
            throw new IllegalArgumentException("You can't delete your active submission");
        }

        submission.softDelete();
        submissionRepository.save(submission);

        storageService.delete(submission.getFileRecord().getUuid().toString());
    }

    @Transactional
    public void onSubmissionValidationComplete(Submission submission, boolean isValid) {
        if(isValid) {
            if (submission.getValidity() == SubmissionValidity.not_evaluated_autoset) {
                submission.getTeam().setCurrentSubmission(submission);
                teamRepository.save(submission.getTeam());
            }

            submission.setValidity(SubmissionValidity.valid);
            submissionRepository.save(submission);
        } else {
            submission.setValidity(SubmissionValidity.invalid);
            submissionRepository.save(submission);
        }
    }

    public Optional<Submission> getSubmission(UUID uuid) {
        return submissionRepository.findSubmissionByUuid(uuid);
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

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File is too large");
        }

        String contentType = file.getContentType();
    }
}

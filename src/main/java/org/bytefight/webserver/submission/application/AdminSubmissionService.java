package org.bytefight.webserver.submission.application;

import org.bytefight.webserver.submission.application.SubmissionService;
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

import java.io.IOException;

@Service
public class AdminSubmissionService {
    private final SubmissionRepository submissionRepository;
    private final TeamRepository teamRepository;
    private final SubmissionService submissionService;

    public AdminSubmissionService(
            SubmissionRepository submissionRepository,
            TeamRepository teamRepository,
            SubmissionService submissionService
    ) {
        this.submissionRepository = submissionRepository;
        this.teamRepository = teamRepository;
        this.submissionService = submissionService;
    }

    public Page<Submission> listSubmissions(boolean isDeleted, Pageable pageable) {
        return submissionRepository.findByIsDeleted(isDeleted, pageable);
    }

    public Submission createSubmission(AdminCreateSubmissionDto input) throws IOException {
        Team team = teamRepository.findById(input.getTeamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
        Submission submission = submissionService.createSubmission(team, input.getDescription(), input.getFile(), false);
        if (input.getValidity() != null) {
            submission.setValidity(input.getValidity());
            submission = submissionRepository.save(submission);
        }
        return submission;
    }
}

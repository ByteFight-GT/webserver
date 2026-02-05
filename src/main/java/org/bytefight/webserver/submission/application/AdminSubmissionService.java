package org.bytefight.webserver.submission.application;

import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AdminSubmissionService {
    private final SubmissionRepository submissionRepository;

    public AdminSubmissionService(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public Page<Submission> listSubmissions(boolean isDeleted, Pageable pageable) {
        return submissionRepository.findByIsDeleted(isDeleted, pageable);
    }
}

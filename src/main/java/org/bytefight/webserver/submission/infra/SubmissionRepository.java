package org.bytefight.webserver.submission.infra;

import org.bytefight.webserver.submission.domain.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findSubmissionsByTeamIdOrderByCreatedAtDesc(Long teamId);
    Optional<Submission> findSubmissionByUuid(UUID uuid);
    boolean existsByUuid(UUID uuid);
    Page<Submission> findByIsDeleted(boolean isDeleted, Pageable pageable);
}

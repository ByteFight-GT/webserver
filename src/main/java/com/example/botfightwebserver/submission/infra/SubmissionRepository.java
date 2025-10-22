package com.example.botfightwebserver.submission.infra;

import com.example.botfightwebserver.submission.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findSubmissionsByTeamIdOrderByCreatedAtDesc(Long teamId);
}

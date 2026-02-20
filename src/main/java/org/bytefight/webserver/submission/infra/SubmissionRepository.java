package org.bytefight.webserver.submission.infra;

import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findSubmissionsByTeamAndIsDeletedIsFalseOrderByCreatedAtDesc(Team team);
    Optional<Submission> findSubmissionByTeamAndUuidAndIsDeletedIsFalse(Team team, UUID uuid);
    Optional<Submission> findSubmissionByUuid(UUID uuid);
    boolean existsByUuid(UUID uuid);
    Page<Submission> findByIsDeleted(boolean isDeleted, Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(fr.size), 0)
        FROM Submission s
        JOIN s.fileRecord fr
        WHERE s.team = :team
          AND s.isDeleted = false
    """)
    Long sumUndeletedSubmissionSizeByTeam(@Param("team") Team team);
}

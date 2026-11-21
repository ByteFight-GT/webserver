package org.bytefight.webserver.submission.infra;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
  List<Submission> findSubmissionsByTeamAndDeletedAtNullOrderByCreatedAtDesc(Team team);

  @Query(
      """
        SELECT s, gm
        FROM Submission s
        JOIN FETCH s.fileRecord
        JOIN FETCH s.team t
        JOIN FETCH t.competition
        LEFT JOIN GameMatch gm
          ON gm.id = (
            SELECT MAX(gm2.id)
            FROM GameMatch gm2
            WHERE gm2.submissionA = s
              AND gm2.status IN :statuses
          )
        WHERE s.team = :team
          AND s.deletedAt IS NULL
        ORDER BY s.createdAt DESC
    """)
  List<Object[]> findWithLatestMatchInStatusesByTeam(
      @Param("team") Team team, @Param("statuses") Collection<MatchStatus> statuses);

  List<MatchStatus> VALIDATION_RESULT_STATUSES =
      List.of(MatchStatus.submission_valid, MatchStatus.submission_invalid);

  /**
   * Lists a team's undeleted submissions, newest first, each with its latest finished validation
   * match. Each row is {@code [Submission, GameMatch]}, where the match may be null.
   */
  default List<Object[]> findWithValidationMatchByTeam(Team team) {
    return findWithLatestMatchInStatusesByTeam(team, VALIDATION_RESULT_STATUSES);
  }

  Optional<Submission> findSubmissionByTeamAndUuidAndDeletedAtNull(Team team, UUID uuid);

  Optional<Submission> findSubmissionByUuidAndDeletedAtNull(UUID uuid);

  boolean existsByUuid(UUID uuid);

  @Query(
      """
        SELECT s FROM Submission s
        WHERE (:isDeleted = true AND s.deletedAt IS NOT NULL)
           OR (:isDeleted = false AND s.deletedAt IS NULL)
    """)
  Page<Submission> findByIsDeleted(@Param("isDeleted") boolean isDeleted, Pageable pageable);

  @Query(
      """
        SELECT COALESCE(SUM(fr.size), 0)
        FROM Submission s
        JOIN s.fileRecord fr
        WHERE s.team = :team
          AND s.deletedAt IS NULL
    """)
  Long sumUndeletedSubmissionSizeByTeam(@Param("team") Team team);
}

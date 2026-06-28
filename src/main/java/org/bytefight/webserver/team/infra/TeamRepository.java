package org.bytefight.webserver.team.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
  boolean existsByCompetitionAndNameNormalized(Competition competition, String nameNormalized);

  boolean existsByJoinCode(String joinCode);

  List<Team> findAllByCompetition(Competition competition);

  Optional<Team> findByUuidAndDeletedAtNull(UUID uuid);

  Optional<Team> findByCompetitionAndJoinCodeAndDeletedAtNull(
      Competition competition, String joinCode);

  int countByCurrentSubmissionNotNull();

  Optional<Team> findByJoinCode(String joinCode);

  Optional<Team> findByUuid(UUID uuid);

  boolean existsByUuid(UUID uuid);

  List<Team> findAllByDeletedAtNullAndCurrentSubmissionIsNotNullAndCompetition(
      Competition competition);

  @Query(
      """
        SELECT t FROM Team t
        WHERE (:isDeleted = true AND t.deletedAt IS NOT NULL)
           OR (:isDeleted = false AND t.deletedAt IS NULL)
    """)
  Page<Team> findByIsDeleted(@Param("isDeleted") boolean isDeleted, Pageable pageable);

  @Query(
      """
        SELECT t FROM Team t
        WHERE t.competition.id = :competitionId
          AND ((:isDeleted = true AND t.deletedAt IS NOT NULL)
            OR (:isDeleted = false AND t.deletedAt IS NULL))
    """)
  Page<Team> findByCompetitionIdAndIsDeleted(
      @Param("competitionId") Long competitionId,
      @Param("isDeleted") boolean isDeleted,
      Pageable pageable);

  Page<Team> findByIdIn(List<Long> ids, Pageable pageable);

  Page<Team> findByCompetitionIdAndIdIn(Long competitionId, List<Long> ids, Pageable pageable);

  //    Optional<Integer> findRankByUuid(UUID uuid);

  Optional<Team> findByCompetitionAndUuid(Competition competition, UUID uuid);

  Optional<Team> findByCompetitionAndNameNormalizedAndDeletedAtNull(
      Competition competition, String nameNormalized);
}

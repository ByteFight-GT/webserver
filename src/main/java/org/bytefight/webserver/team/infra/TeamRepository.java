package org.bytefight.webserver.team.infra;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByCompetitionAndNameNormalized(Competition competition, String nameNormalized);
    boolean existsByJoinCode(String joinCode);

    List<Team> findAllByCompetition(Competition competition);

    Optional<Team> findByUuidAndIsDeletedFalse(UUID uuid);
    Optional<Team> findByCompetitionAndJoinCodeAndIsDeletedIsFalse(Competition competition, String joinCode);

    int countByCurrentSubmissionNotNull();
    Optional<Team> findByJoinCode(String joinCode);
    Optional<Team> findByUuid(UUID uuid);
    boolean existsByUuid(UUID uuid);

    List<Team> findAllByIsDeletedFalseAndCurrentSubmissionIsNotNullAndCompetition(Competition competition);

    Page<Team> findByIsDeleted(boolean isDeleted, Pageable pageable);
    Page<Team> findByCompetitionIdAndIsDeleted(Long competitionId, boolean isDeleted, Pageable pageable);
    Page<Team> findByIdIn(List<Long> ids, Pageable pageable);
    Page<Team> findByCompetitionIdAndIdIn(Long competitionId, List<Long> ids, Pageable pageable);

//    Optional<Integer> findRankByUuid(UUID uuid);

    Optional<Team> findByCompetitionAndUuid(Competition competition, UUID uuid);
}

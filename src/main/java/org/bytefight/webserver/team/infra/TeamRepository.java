package org.bytefight.webserver.team.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long>, JpaSpecificationExecutor<Team> {
  boolean existsByCompetitionAndNameNormalized(Competition competition, String nameNormalized);

  boolean existsByJoinCode(String joinCode);

  List<Team> findAllByCompetition(Competition competition);

  Optional<Team> findByUuidAndIsDeletedFalse(UUID uuid);

  Optional<Team> findByCompetitionAndJoinCodeAndIsDeletedIsFalse(
      Competition competition, String joinCode);

  int countByCurrentSubmissionNotNull();

  Optional<Team> findByJoinCode(String joinCode);

  Optional<Team> findByUuid(UUID uuid);

  boolean existsByUuid(UUID uuid);

  List<Team> findAllByIsDeletedFalseAndCurrentSubmissionIsNotNullAndCompetition(
      Competition competition);

  //    Optional<Integer> findRankByUuid(UUID uuid);

  Optional<Team> findByCompetitionAndUuid(Competition competition, UUID uuid);

  Optional<Team> findByCompetitionAndNameNormalizedAndIsDeletedFalse(
      Competition competition, String nameNormalized);
}

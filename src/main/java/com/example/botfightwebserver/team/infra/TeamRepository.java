package com.example.botfightwebserver.team.infra;

import com.example.botfightwebserver.competition.domain.Competition;
import com.example.botfightwebserver.team.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByCompetitionAndNameNormalized(Competition competition, String nameNormalized);
    boolean existsByJoinCode(String joinCode);

    Optional<Team> findByCompetitionAndJoinCodeAndIsDeletedIsFalse(Competition competition, String joinCode);

    int countByCurrentSubmissionNotNull();
    Optional<Team> findByJoinCode(String joinCode);
    Optional<Team> findByUuid(UUID uuid);
    Optional<Team> findByUuidAndIsDeletedFalse(UUID uuid);
    boolean existsByUuid(UUID uuid);

    List<Team> findAllByIsDeletedFalse();

//    Optional<Integer> findRankByUuid(UUID uuid);

    Optional<Team> findByCompetitionAndUuid(Competition competition, UUID uuid);
}

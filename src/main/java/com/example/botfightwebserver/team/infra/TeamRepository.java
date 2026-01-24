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
    boolean existsByName(String name);
    int countByCurrentSubmissionNotNull();
    Optional<Team> findByJoinCode(String joinCode);
    Optional<Team> findByUuid(UUID uuid);
    Optional<Team> findByUuidAndIsDeletedFalse(UUID uuid);
    boolean existsByUuid(UUID uuid);

    List<Team> findAllByIsDeletedFalse();

    @Query("""
      select 1 + count(t2.id)
      from Team t
      left join Team t2 on (
           t2.isDeleted = false
           and t2.matchesPlayed > 0
       and ( t2.glicko > t.glicko
         or(t2.glicko = t.glicko and t2.matchesPlayed > t.matchesPlayed)
         or(t2.glicko = t.glicko and t2.matchesPlayed = t.matchesPlayed and t2.id > t.id)
       )
      )
      where t.uuid = :uuid and t.isDeleted IS false and t.matchesPlayed > 0
      group by t.id
    """)
    Optional<Integer> findRankByUuid(UUID uuid);

    @Query("SELECT t FROM Team t WHERE NOT t.isDeleted ORDER BY CASE WHEN t.currentSubmission IS NULL THEN 1 ELSE 0 END, t.glicko DESC")
    Page<Team> findTeamsPaginated(Pageable pageable);

    List<Team> findAllByOrderByGlickoDescMatchesPlayedAscIdAsc();

    Optional<Team> findByCompetitionAndUuid(Competition competition, UUID uuid);
}

/*
 *  This is the query for putting unranked people in the last rank
 *  @Query("""
 *       select 1 + count(t2.id)
 *       from Team t
 *       left join Team t2 on (
 *            t2.isDeleted = false
 *        and ( case when t2.matchesPlayed < 1 then 1 else 0 end
 *        < case when t.matchesPlayed < 1 then 1 else 0 end
 *        or( case when t2.matchesPlayed < 1 then 1 else 0 end
 *             = case when t.matchesPlayed < 1 then 1 else 0 end
 *             and t2.glicko > t.glicko
 *        )
 *        or ( case when t2.matchesPlayed < 1 then 1 else 0 end
 *             = case when t.matchesPlayed < 1 then 1 else 0 end
 *             and t2.glicko = t.glicko
 *             and t2.matchesPlayed > t.matchesPlayed
 *        )
 *        or ( case when t2.matchesPlayed < 1 then 1 else 0 end
 *             = case when t.matchesPlayed < 1 then 1 else 0 end
 *             and t2.glicko = t.glicko
 *             and t2.matchesPlayed = t.matchesPlayed
 *             and t2.id > t.id
 *        )
 *        )
 *       )
 *       where t.uuid = :uuid and t.isDeleted = false
 *       group by t.id
 *     """)
 */

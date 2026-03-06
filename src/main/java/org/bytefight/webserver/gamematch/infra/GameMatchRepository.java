package org.bytefight.webserver.gamematch.infra;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameMatchRepository
    extends JpaRepository<GameMatch, Long>, JpaSpecificationExecutor<GameMatch> {
  @Modifying
  @Query(
      value =
          """
                                WITH cte AS (
                                    SELECT id
                                    FROM game_matches gm
                                    WHERE (gm.status = 'waiting' AND gm.scheduled_at < :threshold)
                                    FOR UPDATE SKIP LOCKED
                                )
                                UPDATE game_matches gm
                                SET status = 'scheduling'
                                FROM cte
                                WHERE gm.id = cte.id
                                RETURNING gm.id
                            """,
      nativeQuery = true)
  List<Long> claimAndMarkStaleMatches(LocalDateTime threshold);

  List<GameMatch> findByStatus(MatchStatus status);

  Optional<GameMatch> findByUuid(UUID uuid);

  boolean existsByUuid(UUID uuid);

  @Query(
      "SELECT gm FROM GameMatch gm WHERE (gm.teamA.uuid = :teamUuid OR gm.teamB.uuid = :teamUuid) AND gm.status NOT IN :statusList ORDER BY gm.finishedAt DESC")
  List<GameMatch> findTeamMatches(
      @Param("teamUuid") UUID teamUuid, @Param("statusList") List<MatchStatus> statusList);

  @Query(
      "SELECT gm FROM GameMatch gm WHERE (gm.teamA.id = :teamId OR gm.teamB.id = :teamId) AND gm.reason IN :reasonList ORDER BY gm.finishedAt DESC")
  List<GameMatch> findTeamMatchesByReason(
      @Param("teamUuid") UUID teamId, @Param("reasonList") List<MatchReason> reasonList);

  @Query(
      "SELECT gm FROM GameMatch gm WHERE (gm.teamA.uuid = :teamUuid OR gm.teamB.uuid = :teamUuid) AND gm.status NOT IN :statusList AND gm.reason NOT IN :reasonList ORDER BY gm.finishedAt DESC")
  Page<GameMatch> findTeamMatches(
      @Param("teamUuid") UUID teamUuid,
      @Param("statusList") List<MatchStatus> statusList,
      @Param("reasonList") List<MatchReason> reasonList,
      Pageable pageable);

  @Query(
      """
                SELECT COUNT(gm)
                FROM GameMatch gm
                WHERE (gm.teamA = :team OR gm.teamB = :team)
                  AND gm.ladder = :ladder
                  AND gm.status IN :status
            """)
  long countTeamMatchesByLadderAndStatus(
      @Param("team") Team team,
      @Param("ladder") String ladder,
      @Param("status") Collection<MatchStatus> status);
}

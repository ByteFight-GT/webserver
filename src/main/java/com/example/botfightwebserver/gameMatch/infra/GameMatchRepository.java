package com.example.botfightwebserver.gameMatch.infra;

import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.gameMatch.domain.MatchReason;
import com.example.botfightwebserver.gameMatch.domain.MatchStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface GameMatchRepository extends JpaRepository<GameMatch, Long>, JpaSpecificationExecutor<GameMatch> {
    @Modifying
    @Query(value = """
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
    """, nativeQuery = true)
    List<Long> claimAndMarkStaleMatches(LocalDateTime threshold);

    List<GameMatch> findByStatus(MatchStatus status);

    Optional<GameMatch> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    @Query("SELECT gm FROM GameMatch gm WHERE (gm.teamA.uuid = :teamUuid OR gm.teamB.uuid = :teamUuid) AND gm.status NOT IN :statusList ORDER BY gm.finishedAt DESC")
    List<GameMatch> findTeamMatches(@Param("teamUuid") UUID teamUuid, @Param("statusList") List<MatchStatus> statusList);

    @Query("SELECT gm FROM GameMatch gm WHERE (gm.teamA.id = :teamId OR gm.teamB.id = :teamId) AND gm.reason IN :reasonList ORDER BY gm.finishedAt DESC")
    List<GameMatch> findTeamMatchesByReason(@Param("teamId") Long teamId, @Param("reasonList") List<MatchReason> reasonList);

    @Query("SELECT gm FROM GameMatch gm WHERE (gm.teamA.uuid = :teamUuid OR gm.teamB.uuid = :teamUuid) AND gm.status NOT IN :statusList AND gm.reason NOT IN :reasonList ORDER BY gm.finishedAt DESC")
    Page<GameMatch> findTeamMatches(@Param("teamUuid") UUID teamUuid, @Param("statusList") List<MatchStatus> statusList, @Param("reasonList") List<MatchReason> reasonList, Pageable pageable);

    @Query("SELECT gm FROM GameMatch gm WHERE (gm.teamA.uuid = :teamUuid OR gm.teamB.uuid = :teamUuid) AND (gm.teamA.uuid = :otherTeamUuid OR gm.teamB.uuid = :otherTeamUuid) AND gm.status NOT IN :statusList AND gm.reason NOT IN :reasonList ORDER BY gm.finishedAt DESC")
    Page<GameMatch> findTeamMatches(@Param("teamUuid") UUID teamUuid, @Param("otherTeamUuid") UUID otherTeamUuid, @Param("statusList") List<MatchStatus> statusList, @Param("reasonList") List<MatchReason> reasonList, Pageable pageable);
}

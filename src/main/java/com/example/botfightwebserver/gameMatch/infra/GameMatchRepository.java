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
            FROM game_match gm
            WHERE (gm.status = 'WAITING' AND gm.queued_at < :threshold)
            FOR UPDATE SKIP LOCKED
        )
        UPDATE game_match gm
        SET status = 'RESCHEDULING'
        FROM cte
        WHERE gm.id = cte.id
        RETURNING gm.id
    """, nativeQuery = true)
    List<Long> claimAndMarkStaleMatches(LocalDateTime threshold);

    Page<GameMatch> findAllByOrderByProcessedAt(@Nullable Specification<GameMatch> spec, Pageable pageable);

    List<GameMatch> findByStatus(MatchStatus status);

    Optional<GameMatch> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    @Query("SELECT gm FROM GameMatch gm WHERE (gm.teamOne.uuid = :teamUuid OR gm.teamTwo.uuid = :teamUuid) AND gm.status NOT IN :statusList ORDER BY gm.processedAt DESC")
    List<GameMatch> findTeamMatches(@Param("teamUuid") UUID teamUuid, @Param("statusList") List<MatchStatus> statusList);

    @Query("SELECT gm FROM GameMatch gm WHERE (gm.teamOne.id = :teamId OR gm.teamTwo.id = :teamId) AND gm.reason IN :reasonList ORDER BY gm.processedAt DESC")
    List<GameMatch> findTeamMatchesByReason(@Param("teamId") Long teamId, @Param("reasonList") List<MatchReason> reasonList);

    @Query("SELECT gm FROM GameMatch gm WHERE (gm.teamOne.uuid = :teamUuid OR gm.teamTwo.uuid = :teamUuid) AND gm.status NOT IN :statusList AND gm.reason NOT IN :reasonList ORDER BY gm.processedAt DESC")
    Page<GameMatch> findTeamMatches(@Param("teamUuid") UUID teamUuid, @Param("statusList") List<MatchStatus> statusList, @Param("reasonList") List<MatchReason> reasonList, Pageable pageable);

    @Query("SELECT gm FROM GameMatch gm WHERE (gm.teamOne.uuid = :teamUuid OR gm.teamTwo.uuid = :teamUuid) AND (gm.teamOne.uuid = :otherTeamUuid OR gm.teamTwo.uuid = :otherTeamUuid) AND gm.status NOT IN :statusList AND gm.reason NOT IN :reasonList ORDER BY gm.processedAt DESC")
    Page<GameMatch> findTeamMatches(@Param("teamUuid") UUID teamUuid, @Param("otherTeamId") UUID otherTeamUuid, @Param("statusList") List<MatchStatus> statusList, @Param("reasonList") List<MatchReason> reasonList, Pageable pageable);
}


package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.team.StatsDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface GameMatchRepository extends JpaRepository<GameMatch, Long> {
    List<GameMatch> findByStatusAndQueuedAtBefore(MATCH_STATUS status, LocalDateTime threshold);

    List<GameMatch> findByStatus(MATCH_STATUS status);

    @Query("SELECT gm FROM GameMatch gm WHERE (gm.teamOne.id = :teamId OR gm.teamTwo.id = :teamId) AND gm.status NOT IN :statusList")
    List<GameMatch> findTeamMatches(@Param("teamId") Long teamId, @Param("statusList") List<MATCH_STATUS> statusList);

    @Query("SELECT gm FROM GameMatch gm WHERE (gm.teamOne.id = :teamId OR gm.teamTwo.id = :teamId) AND gm.reason IN :reasonList")
    List<GameMatch> findTeamMatchesByReason(@Param("teamId") Long teamId, @Param("reasonList") List<MATCH_REASON> reasonList);

    @Query("SELECT gm FROM GameMatch gm WHERE (gm.teamOne.id = :teamId OR gm.teamTwo.id = :teamId) AND gm.status NOT IN :statusList AND gm.reason NOT IN :reasonList")
    Page<GameMatch> findTeamMatches(@Param("teamId") Long teamId, @Param("statusList") List<MATCH_STATUS> statusList, @Param("reasonList") List<MATCH_REASON> reasonList, Pageable pageable);

    @Query("SELECT gm FROM GameMatch gm WHERE (gm.teamOne.id = :teamId OR gm.teamTwo.id = :teamId) AND (gm.teamOne.id = :otherTeamId OR gm.teamTwo.id = :otherTeamId) AND gm.status NOT IN :statusList AND gm.reason NOT IN :reasonList")
    Page<GameMatch> findTeamMatches(@Param("teamId") Long teamId, @Param("otherTeamId") Long otherTeamId, @Param("statusList") List<MATCH_STATUS> statusList, @Param("reasonList") List<MATCH_REASON> reasonList, Pageable pageable);
}


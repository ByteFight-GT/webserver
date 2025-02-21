package com.example.botfightwebserver.gameMatch;

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

    @Query("SELECT gm FROM GameMatch gm WHERE (gm.teamOne.id = :teamId OR gm.teamTwo.id = :teamId) AND gm.status != :status")
    List<GameMatch> findTeamMatches(@Param("teamId") Long teamId, @Param("status") MATCH_STATUS status);

    @Query("SELECT gm FROM GameMatch gm WHERE (gm.teamOne.id = :teamId OR gm.teamTwo.id = :teamId) AND gm.status != :status")
    Page<GameMatch> findTeamMatches(@Param("teamId") Long teamId, @Param("status") MATCH_STATUS status, Pageable pageable);

    @Query("SELECT gm FROM GameMatch gm WHERE (gm.teamOne.id = :teamId OR gm.teamTwo.id = :teamId) AND (gm.teamOne.id = :otherTeamId OR gm.teamTwo.id = :otherTeamId) AND gm.status != :status")
    Page<GameMatch> findTeamMatches(@Param("teamId") Long teamId, @Param("otherTeamId") Long otherTeamId, @Param("status") MATCH_STATUS status, Pageable pageable);

}


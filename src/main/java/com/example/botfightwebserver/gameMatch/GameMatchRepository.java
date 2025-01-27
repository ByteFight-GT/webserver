package com.example.botfightwebserver.gameMatch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface GameMatchRepository extends JpaRepository<GameMatch, Long> {
    List<GameMatch> findByStatusAndQueuedAtBefore(MATCH_STATUS status, LocalDateTime threshold);

    List<GameMatch> findByStatus(MATCH_STATUS status);

    List<GameMatch> findByTeamOne_IdOrTeamTwo_IdAndStatusNotOrderByProcessedAtDesc(Long teamOneId, Long teamTwoId, MATCH_STATUS status);

    Page<GameMatch> findByTeamOne_IdOrTeamTwo_IdAndStatusIsNot(Long teamOneId, Long teamTwoId, MATCH_STATUS status, Pageable pageable);
}
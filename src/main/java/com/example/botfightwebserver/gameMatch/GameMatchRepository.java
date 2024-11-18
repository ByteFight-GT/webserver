package com.example.botfightwebserver.gameMatch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameMatchRepository extends JpaRepository<GameMatch, Long> {
    List<GameMatch> findByStatusAndQueuedAtBefore(MATCH_STATUS status, LocalDateTime threshold);

    List<GameMatch> findByStatus(MATCH_STATUS status);
}

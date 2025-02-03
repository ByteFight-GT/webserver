package com.example.botfightwebserver.matchMaking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchMakingEventRepository extends JpaRepository<MatchMakingEvent, Long> {
    Optional<MatchMakingEvent> findFirstByReasonOrderByCreationDateTimeDesc(MATCHMAKING_REASON reason);
}

package com.example.botfightwebserver.matchMaking.infra;

import com.example.botfightwebserver.matchMaking.domain.MATCHMAKING_REASON;
import com.example.botfightwebserver.matchMaking.domain.MatchMakingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchMakingEventRepository extends JpaRepository<MatchMakingEvent, Long> {
    Optional<MatchMakingEvent> findFirstByReasonOrderByCreationDateTimeDesc(MATCHMAKING_REASON reason);
}

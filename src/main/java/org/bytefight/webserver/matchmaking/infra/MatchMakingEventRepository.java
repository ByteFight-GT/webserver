package org.bytefight.webserver.matchmaking.infra;

import org.bytefight.webserver.matchmaking.domain.MatchmakingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchMakingEventRepository extends JpaRepository<MatchmakingEvent, Long> {
    Optional<MatchmakingEvent> findFirstByOrderByCreatedAtDesc();
}

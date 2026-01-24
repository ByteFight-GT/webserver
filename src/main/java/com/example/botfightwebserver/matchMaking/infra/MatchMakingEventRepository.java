package com.example.botfightwebserver.matchMaking.infra;

import com.example.botfightwebserver.matchMaking.domain.MatchmakingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchMakingEventRepository extends JpaRepository<MatchmakingEvent, Long> {
    Optional<MatchmakingEvent> findFirstByOrderByCreatedAtDesc();
}

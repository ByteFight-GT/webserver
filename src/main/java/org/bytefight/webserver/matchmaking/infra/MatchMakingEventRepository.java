package org.bytefight.webserver.matchmaking.infra;

import java.util.Optional;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.matchmaking.domain.MatchmakingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchMakingEventRepository extends JpaRepository<MatchmakingEvent, Long> {
  Optional<MatchmakingEvent> findFirstByOrderByCreatedAtDesc();

  Optional<MatchmakingEvent> findFirstByCompetitionAndLadderOrderByCreatedAtDesc(
      Competition competition, String ladder);
}

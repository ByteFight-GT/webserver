package org.bytefight.webserver.matchmaking.application;

import lombok.RequiredArgsConstructor;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.matchmaking.domain.MatchmakingEvent;
import org.bytefight.webserver.matchmaking.infra.MatchMakingEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchmakingEventCreator {
  private final MatchMakingEventRepository repository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public MatchmakingEvent create(Competition competition, String ladder) {
    MatchmakingEvent event =
        MatchmakingEvent.builder().competition(competition).ladder(ladder).build();
    return repository.save(event);
  }
}

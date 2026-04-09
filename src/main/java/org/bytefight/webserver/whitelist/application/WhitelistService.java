package org.bytefight.webserver.whitelist.application;

import lombok.RequiredArgsConstructor;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.user.domain.User;
import org.bytefight.webserver.whitelist.infra.WhitelistEntryRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WhitelistService {
  private final WhitelistEntryRepository whitelistEntryRepository;

  public boolean isCompetitionParticipationAllowed(Competition competition, User user) {
    if (!competition.isWhitelisted()) return true;

    return whitelistEntryRepository.existsByCompetitionAndEmail(competition, user.getEmail());
  }
}

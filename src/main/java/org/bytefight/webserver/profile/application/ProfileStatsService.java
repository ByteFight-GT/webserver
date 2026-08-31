package org.bytefight.webserver.profile.application;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.profile.domain.ProfileStats;
import org.bytefight.webserver.profile.infra.ProfileStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileStatsService {
  private final ProfileStatsRepository profileStatsRepository;

  public Optional<ProfileStats> getStatsByPlayer(Player player) {
    return profileStatsRepository.findByPlayer(player);
  }

  @Transactional
  public int recomputeAll() {
    return profileStatsRepository.recomputeGamesPlayed();
  }
}

package org.bytefight.webserver.profile.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileStatsRecomputeJob {
  private final ProfileStatsService profileStatsService;

  @Scheduled(fixedRateString = "${profile-stats.recompute-rate-ms:600000}")
  public void recomputeProfileStats() {
    int rows = profileStatsService.recomputeAll();
    log.info("Recomputed profile stats for {} players", rows);
  }
}

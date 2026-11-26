package org.bytefight.webserver.globalstats.application;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bytefight.webserver.globalstats.domain.GlobalStat;
import org.bytefight.webserver.globalstats.infra.GlobalStatRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GlobalStatsService {
  private static final Pattern METRIC_SLUG = Pattern.compile("[a-z0-9_]+");

  private final GlobalStatRepository globalStatRepository;
  private final List<StatCollector> statCollectors;

  public GlobalStatsService(
      GlobalStatRepository globalStatRepository, List<StatCollector> statCollectors) {
    this.globalStatRepository = globalStatRepository;
    this.statCollectors = statCollectors;
    validateMetrics(statCollectors);
  }

  /**
   * Recomputes every registered stat and upserts it into the global_stats table. A collector that
   * throws is logged and skipped so it can't block the others; its previous value is kept.
   */
  @Scheduled(fixedRateString = "${global-stats.refresh-rate-ms:600000}")
  public void refreshAll() {
    int refreshed = 0;
    for (StatCollector collector : statCollectors) {
      try {
        upsert(collector.metric(), collector.collect());
        refreshed++;
      } catch (Exception e) {
        log.error("Failed to refresh global stat '{}'", collector.metric(), e);
      }
    }
    log.info("Refreshed {}/{} global stats", refreshed, statCollectors.size());
  }

  /** Returns every cached stat, keyed by metric slug. */
  public Map<String, Long> getAllStats() {
    return globalStatRepository.findAll().stream()
        .collect(Collectors.toMap(GlobalStat::getMetric, GlobalStat::getValue));
  }

  private void upsert(String metric, long value) {
    GlobalStat stat =
        globalStatRepository
            .findByMetric(metric)
            .orElseGet(() -> GlobalStat.builder().metric(metric).build());
    stat.setValue(value);
    globalStatRepository.save(stat);
  }

  /** Fails startup on malformed or duplicate slugs, since both would corrupt the table. */
  private static void validateMetrics(List<StatCollector> statCollectors) {
    Set<String> seen = new HashSet<>();
    for (StatCollector collector : statCollectors) {
      String metric = collector.metric();
      if (metric == null || !METRIC_SLUG.matcher(metric).matches()) {
        throw new IllegalStateException(
            "Invalid global stat metric slug '%s' from %s"
                .formatted(metric, collector.getClass().getName()));
      }
      if (!seen.add(metric)) {
        throw new IllegalStateException("Duplicate global stat metric slug '%s'".formatted(metric));
      }
    }
  }
}

package org.bytefight.webserver.globalstats.application;

/**
 * Computes a single global stat. Implementations registered as Spring beans are picked up
 * automatically by {@link GlobalStatsService} and refreshed on its schedule.
 */
public interface StatCollector {
  /** All-lowercase slug identifying the metric (e.g. {@code total_matches_played}). */
  String metric();

  /** Computes the current value of the metric. */
  long collect();
}

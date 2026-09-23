package org.bytefight.webserver.globalstats.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.bytefight.webserver.common.domain.BaseEntity;

/**
 * A single cached platform-wide statistic, keyed by a lowercase metric slug.
 *
 * <p>Why this table: computing global aggregates (e.g. total matches played) on every request is
 * expensive, so values are precomputed and cached here, one row per metric.
 */
@Entity
@Table(name = "global_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalStat extends BaseEntity {

  /** All-lowercase slug identifying the metric (e.g. {@code total_matches_played}). */
  @Column(name = "metric", nullable = false, unique = true)
  private String metric;

  @Column(name = "value", nullable = false)
  private Long value;
}

package org.bytefight.webserver.scrim.infra;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Scrim volume budget. Starting values are 20/day, 60/week, burst 2; tune from the {@code
 * scrim_usage} table rather than debating them up front.
 */
@Data
@Component
@ConfigurationProperties(prefix = "scrim")
public class ScrimProperties {
  /** Maximum scrim matches a team may start per UTC day. */
  private int dailyCap = 20;

  /** Maximum scrim matches a team may start per UTC week (Monday-based). */
  private int weeklyCap = 60;

  /** Maximum scrim matches a team may have in flight at once. */
  private int burst = 2;

  /** Retry-After hint, in seconds, when a request is rejected for hitting the burst cap. */
  private int burstRetryAfterSeconds = 30;
}

package org.bytefight.webserver.scrim.infra;

import java.time.Instant;
import java.util.OptionalInt;

public interface ScrimUsageRepositoryCustom {

  /**
   * Atomically debit one unit against the (team, windowKind, windowStart) budget.
   *
   * <p>Runs a single {@code INSERT ... ON CONFLICT DO UPDATE SET count = count + 1 WHERE count &lt;
   * cap RETURNING count}. Returns the new count when the increment succeeded, or empty when the cap
   * was already reached (the conflicting update's WHERE fails, so the statement returns no row).
   * There is no read-then-write window.
   */
  OptionalInt tryIncrement(long teamId, String windowKind, Instant windowStart, int cap);
}

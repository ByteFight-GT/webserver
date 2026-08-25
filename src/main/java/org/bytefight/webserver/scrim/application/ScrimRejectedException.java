package org.bytefight.webserver.scrim.application;

import lombok.Getter;

/**
 * Thrown by {@link ScrimService#scheduleOneOrThrow} when a scrim unit can't be granted. Because it
 * propagates out of the {@code @Transactional} boundary, any budget increment already applied in
 * that unit is rolled back (e.g. a weekly-cap rejection undoes the daily increment).
 */
@Getter
public class ScrimRejectedException extends RuntimeException {
  public enum Reason {
    BURST,
    DAILY,
    WEEKLY
  }

  private final Reason reason;
  private final long retryAfterSeconds;

  public ScrimRejectedException(Reason reason, long retryAfterSeconds) {
    super("scrim rejected: " + reason);
    this.reason = reason;
    this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
  }
}

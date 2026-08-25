package org.bytefight.webserver.scrim.domain;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/** The budget windows a scrim request is metered against. */
public enum ScrimWindow {
  daily {
    @Override
    public Instant startOf(Instant now) {
      return now.truncatedTo(ChronoUnit.DAYS);
    }

    @Override
    public Instant nextStartAfter(Instant now) {
      return startOf(now).plus(1, ChronoUnit.DAYS);
    }
  },
  weekly {
    @Override
    public Instant startOf(Instant now) {
      return LocalDate.ofInstant(now, ZoneOffset.UTC)
          .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
          .atStartOfDay(ZoneOffset.UTC)
          .toInstant();
    }

    @Override
    public Instant nextStartAfter(Instant now) {
      return LocalDate.ofInstant(now, ZoneOffset.UTC)
          .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
          .atStartOfDay(ZoneOffset.UTC)
          .toInstant();
    }
  };

  /** Start of the window containing {@code now} (UTC). */
  public abstract Instant startOf(Instant now);

  /** Start of the next window after {@code now} — used for the Retry-After hint. */
  public abstract Instant nextStartAfter(Instant now);
}

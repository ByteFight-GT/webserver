package org.bytefight.webserver.scrim.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

import org.bytefight.webserver.team.domain.Team;

/**
 * One windowed usage counter per (team, window kind, window start). Incremented atomically by an
 * {@code INSERT ... ON CONFLICT DO UPDATE ... WHERE count &lt; cap} upsert (see {@code
 * ScrimUsageRepositoryCustom}), so it doubles as the scrim audit trail and the only telemetry
 * available today.
 *
 * <p>This entity is deliberately not a {@code BaseEntity}: it is mutated by native upsert, not by
 * the JPA lifecycle, so there is no meaningful {@code updated_at}.
 */
@Getter
@Setter
@Entity
@Table(
    name = "scrim_usage",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_scrim_usage_team_window",
          columnNames = {"team_id", "window_kind", "window_start"})
    })
public class ScrimUsage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
  private Instant createdAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "team_id", nullable = false)
  private Team team;

  @Column(name = "window_kind", nullable = false)
  private String windowKind;

  @Column(name = "window_start", nullable = false)
  private Instant windowStart;

  @Column(name = "count", nullable = false)
  private int count;
}

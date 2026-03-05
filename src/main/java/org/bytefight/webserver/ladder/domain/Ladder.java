package org.bytefight.webserver.ladder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.competition.domain.Competition;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ladders")
public class Ladder extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "competition_id", nullable = false)
  private Competition competition;

  @Column(name = "ladder", nullable = false, length = 50)
  private String ladder;

  @Column(name = "max_queued_per_team", nullable = false)
  private int maxQueuedPerTeam;

  @Column(name = "allow_user_matches", nullable = false)
  private boolean allowUserMatches = false;

  @Column(name = "scheduled_matchmaking_enabled", nullable = false)
  private boolean scheduledMatchmakingEnabled = false;

  @Column(name = "scheduled_matchmaking_cron", nullable = true)
  private String scheduledMatchmakingCron;

  @Column(name = "glicko_default_rating", nullable = false)
  private double glickoDefaultRating;

  @Column(name = "glicko_default_rd", nullable = false)
  private double glickoDefaultRd;

  @Column(name = "glicko_rd_max", nullable = false)
  private double glickoRdMax;

  @Column(name = "glicko_rd_min")
  private Double glickoRdMin;

  @Column(name = "glicko_phi_inflation_per_day", nullable = false)
  @Builder.Default
  private double glickoPhiInflationPerDay = 0.0;

  @Column(name = "glicko_tau", nullable = false)
  private double glickoTau;

  @Column(name = "glicko_sigma_default", nullable = false)
  private double glickoSigmaDefault;

  @Column(name = "glicko_sigma_min")
  private Double glickoSigmaMin;

  @Column(name = "glicko_sigma_max")
  private Double glickoSigmaMax;
}

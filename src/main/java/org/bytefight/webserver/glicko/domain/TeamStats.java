package org.bytefight.webserver.glicko.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.team.domain.Team;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(
    name = "team_stats",
    indexes = {
      @Index(name = "uk_team_stats_team_ladder", columnList = "team_id,ladder", unique = true)
    })
public class TeamStats extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "team_id", nullable = false)
  private Team team;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "competition_id", nullable = false)
  private Competition competition;

  @Column(name = "ladder", nullable = false, length = 50)
  private String ladder;

  @Column(name = "matches_played", nullable = false)
  @Builder.Default
  private int matchesPlayed = 0;

  @Column(name = "wins", nullable = false)
  @Builder.Default
  private int wins = 0;

  @Column(name = "losses", nullable = false)
  @Builder.Default
  private int losses = 0;

  @Column(name = "draws", nullable = false)
  @Builder.Default
  private int draws = 0;

  @Column(name = "glicko_rating", nullable = false)
  private double glickoRating;

  @Column(name = "glicko_rd", nullable = false)
  private double glickoRd;

  @Column(name = "glicko_volatility", nullable = false)
  private double glickoVolatility;
}

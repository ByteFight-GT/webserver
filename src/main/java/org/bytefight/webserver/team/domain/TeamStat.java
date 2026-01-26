package org.bytefight.webserver.team.domain;

import org.bytefight.webserver.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "team_stats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_team_stats_team_ladder",
                        columnNames = {"team_id", "ladder"}
                )
        }
)
public class TeamStat extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "ladder", nullable = false, length = 50)
    private String ladder;

    @Column(name = "matches_played", nullable = false)
    private int matchesPlayed = 0;

    @Column(name = "wins", nullable = false)
    private int wins = 0;

    @Column(name = "losses", nullable = false)
    private int losses = 0;

    @Column(name = "draws", nullable = false)
    private int draws = 0;

    @Column(name = "glicko_rating", nullable = false, precision = 7, scale = 2)
    private BigDecimal glickoRating;

    @Column(name = "glicko_rd", nullable = false)
    private double glickoRd;

    @Column(name = "glicko_volatility", nullable = false)
    private double glickoVolatility;
}
package org.bytefight.webserver.glicko.domain;

import lombok.*;
import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.team.domain.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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
        }
)
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

    public Team getTeam() {
        return team;
    }

    public int getMatchesPlayed() {
        return matchesPlayed;
    }

    public void setMatchesPlayed(int matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public int getDraws() {
        return draws;
    }

    public void setDraws(int draws) {
        this.draws = draws;
    }

    public double getGlickoRating() {
        return glickoRating;
    }

    public void setGlickoRating(double glickoRating) {
        this.glickoRating = glickoRating;
    }

    public double getGlickoRd() {
        return glickoRd;
    }

    public void setGlickoRd(double glickoRd) {
        this.glickoRd = glickoRd;
    }

    public double getGlickoVolatility() {
        return glickoVolatility;
    }

    public void setGlickoVolatility(double glickoVolatility) {
        this.glickoVolatility = glickoVolatility;
    }
}

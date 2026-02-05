package org.bytefight.webserver.glicko.domain;

import lombok.*;
import org.bytefight.webserver.competition.domain.Competition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ladders")
@IdClass(LadderId.class)
public class Ladder {
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @Id
    @Column(name = "ladder", nullable = false, length = 50)
    private String ladder;

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

    public Competition getCompetition() {
        return competition;
    }

    public void setCompetition(Competition competition) {
        this.competition = competition;
    }

    public String getLadder() {
        return ladder;
    }

    public void setLadder(String ladder) {
        this.ladder = ladder;
    }

    public double getGlickoDefaultRating() {
        return glickoDefaultRating;
    }

    public void setGlickoDefaultRating(double glickoDefaultRating) {
        this.glickoDefaultRating = glickoDefaultRating;
    }

    public double getGlickoDefaultRd() {
        return glickoDefaultRd;
    }

    public void setGlickoDefaultRd(double glickoDefaultRd) {
        this.glickoDefaultRd = glickoDefaultRd;
    }

    public double getGlickoRdMax() {
        return glickoRdMax;
    }

    public void setGlickoRdMax(double glickoRdMax) {
        this.glickoRdMax = glickoRdMax;
    }

    public Double getGlickoRdMin() {
        return glickoRdMin;
    }

    public void setGlickoRdMin(Double glickoRdMin) {
        this.glickoRdMin = glickoRdMin;
    }

    public double getGlickoPhiInflationPerDay() {
        return glickoPhiInflationPerDay;
    }

    public void setGlickoPhiInflationPerDay(double glickoPhiInflationPerDay) {
        this.glickoPhiInflationPerDay = glickoPhiInflationPerDay;
    }

    public double getGlickoTau() {
        return glickoTau;
    }

    public void setGlickoTau(double glickoTau) {
        this.glickoTau = glickoTau;
    }

    public double getGlickoSigmaDefault() {
        return glickoSigmaDefault;
    }

    public void setGlickoSigmaDefault(double glickoSigmaDefault) {
        this.glickoSigmaDefault = glickoSigmaDefault;
    }

    public Double getGlickoSigmaMin() {
        return glickoSigmaMin;
    }

    public void setGlickoSigmaMin(Double glickoSigmaMin) {
        this.glickoSigmaMin = glickoSigmaMin;
    }

    public Double getGlickoSigmaMax() {
        return glickoSigmaMax;
    }

    public void setGlickoSigmaMax(Double glickoSigmaMax) {
        this.glickoSigmaMax = glickoSigmaMax;
    }
}

package org.bytefight.webserver.glicko.domain;

import org.bytefight.webserver.competition.domain.Competition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
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

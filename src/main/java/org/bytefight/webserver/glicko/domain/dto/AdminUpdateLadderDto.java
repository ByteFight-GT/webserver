package org.bytefight.webserver.glicko.domain.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Value;

@Value
public class AdminUpdateLadderDto {
    @Positive Double glickoDefaultRating;
    @Positive Double glickoDefaultRd;
    @Positive Double glickoRdMax;
    @Positive Double glickoRdMin;
    @PositiveOrZero Double glickoPhiInflationPerDay;
    @Positive Double glickoTau;
    @Positive Double glickoSigmaDefault;
    @Positive Double glickoSigmaMin;
    @Positive Double glickoSigmaMax;

    public Double getGlickoDefaultRating() {
        return glickoDefaultRating;
    }

    public Double getGlickoDefaultRd() {
        return glickoDefaultRd;
    }

    public Double getGlickoRdMax() {
        return glickoRdMax;
    }

    public Double getGlickoRdMin() {
        return glickoRdMin;
    }

    public Double getGlickoPhiInflationPerDay() {
        return glickoPhiInflationPerDay;
    }

    public Double getGlickoTau() {
        return glickoTau;
    }

    public Double getGlickoSigmaDefault() {
        return glickoSigmaDefault;
    }

    public Double getGlickoSigmaMin() {
        return glickoSigmaMin;
    }

    public Double getGlickoSigmaMax() {
        return glickoSigmaMax;
    }
}

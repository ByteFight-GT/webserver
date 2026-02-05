package org.bytefight.webserver.glicko.domain.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record AdminUpdateLadderDto(
        @Positive Double glickoDefaultRating,
        @Positive Double glickoDefaultRd,
        @Positive Double glickoRdMax,
        @Positive Double glickoRdMin,
        @PositiveOrZero Double glickoPhiInflationPerDay,
        @Positive Double glickoTau,
        @Positive Double glickoSigmaDefault,
        @Positive Double glickoSigmaMin,
        @Positive Double glickoSigmaMax
) {
}

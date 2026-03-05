package org.bytefight.webserver.ladder.domain.dto;

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
}

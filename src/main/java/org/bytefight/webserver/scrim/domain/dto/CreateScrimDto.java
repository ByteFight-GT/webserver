package org.bytefight.webserver.scrim.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class CreateScrimDto {
  @NotBlank private String competitionSlug;

  /** The TA bot's team name (its slug). The TA bot's UUID never crosses this boundary. */
  @NotBlank private String taBotSlug;

  /** How many scrim matches to try to schedule. Null defaults to 1; bounded by the budget. */
  private Integer count;
}

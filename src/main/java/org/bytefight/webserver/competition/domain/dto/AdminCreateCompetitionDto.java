package org.bytefight.webserver.competition.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class AdminCreateCompetitionDto {
  @NotBlank
  @Size(max = 255)
  @Pattern(
      regexp = "^[a-z0-9_]+$",
      message = "slug must be lowercase alphanumeric with underscores only")
  String slug;

  @NotBlank String name;
}

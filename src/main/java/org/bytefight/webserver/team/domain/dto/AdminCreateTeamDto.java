package org.bytefight.webserver.team.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class AdminCreateTeamDto {
  @NotNull Long competitionId;
  @NotBlank String name;
  String quote;
  Boolean displayMembers;
}

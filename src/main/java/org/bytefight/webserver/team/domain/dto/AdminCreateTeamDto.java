package org.bytefight.webserver.team.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminCreateTeamDto(
        @NotNull Long competitionId,
        @NotBlank String name,
        String quote,
        Boolean displayMembers
) {
}

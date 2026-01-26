package org.bytefight.webserver.team.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class AdminCreateTeamDto {
    @NotNull String name;
    String quote;
    String submissionUuid;
    boolean displayMembers = false;
}

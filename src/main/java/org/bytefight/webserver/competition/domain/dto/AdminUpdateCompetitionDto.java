package org.bytefight.webserver.competition.domain.dto;

import jakarta.validation.constraints.Min;
import lombok.Value;

@Value
public class AdminUpdateCompetitionDto {
    String name;
    String description;
    Boolean isActive;
    Boolean isWhitelisted;

    @Min(1)
    Integer maxPlayersPerTeam;
}

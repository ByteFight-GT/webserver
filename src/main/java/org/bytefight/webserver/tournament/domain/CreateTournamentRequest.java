package org.bytefight.webserver.tournament.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin input payload for creating a tournament.
 */
@Getter
@Setter
public class CreateTournamentRequest {
    @NotBlank
    private String name;

    @NotNull
    private Integer maxTeams;
}

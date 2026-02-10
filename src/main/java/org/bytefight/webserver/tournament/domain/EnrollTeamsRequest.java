package org.bytefight.webserver.tournament.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Admin input payload for enrolling teams.
 * If omitted or empty, all teams with submissions are enrolled.
 */
@Getter
@Setter
public class EnrollTeamsRequest {
    private List<String> teamUuids;
    @NotNull
    @NotBlank
    private String seedLadder; // make it not nullable
}

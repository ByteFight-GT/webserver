package org.bytefight.webserver.tournament.domain;

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
}

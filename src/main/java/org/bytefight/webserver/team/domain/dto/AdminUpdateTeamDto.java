package org.bytefight.webserver.team.domain.dto;

import org.bytefight.webserver.team.domain.TeamType;

public record AdminUpdateTeamDto(
    String name, String quote, Boolean displayMembers, TeamType type) {}

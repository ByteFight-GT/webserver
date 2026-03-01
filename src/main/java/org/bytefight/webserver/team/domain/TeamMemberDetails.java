package org.bytefight.webserver.team.domain;

import java.util.UUID;

public interface TeamMemberDetails {
    Long getTeamId();
    Long getPlayerId();
    Long getPlayerUserId();
    UUID getPlayerUuid();
    String getPlayerUsername();
    String getPlayerEmail();
}

package org.bytefight.webserver.gamematch.domain.dto;

import org.bytefight.webserver.gamematch.domain.MatchStatus;

import java.io.Serializable;

public record GameMatchResult(String matchUuid, MatchStatus status, String matchLog) implements Serializable {
}

package org.bytefight.webserver.gamematch.domain;

import java.io.Serializable;

public record GameMatchResult(String matchUuid, MatchStatus status, String matchLog) implements Serializable {
}

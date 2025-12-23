package com.example.botfightwebserver.gameMatch.domain;

import java.io.Serializable;

public record GameMatchResult(String matchUuid, MatchStatus status, String matchLog) implements Serializable {
}

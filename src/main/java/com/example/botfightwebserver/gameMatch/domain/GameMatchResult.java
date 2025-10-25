package com.example.botfightwebserver.gameMatch.domain;

import java.io.Serializable;

public record GameMatchResult(String matchUuid, MATCH_STATUS status, String matchLog) implements Serializable {
}

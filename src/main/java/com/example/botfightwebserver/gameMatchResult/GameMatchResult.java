package com.example.botfightwebserver.gameMatchResult;

import com.example.botfightwebserver.gameMatch.MATCH_STATUS;

import java.io.Serializable;

public record GameMatchResult(Long matchId, MATCH_STATUS status, String matchLog) implements Serializable {
}

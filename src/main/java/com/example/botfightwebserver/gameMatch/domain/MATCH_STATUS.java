package com.example.botfightwebserver.gameMatch.domain;

public enum MATCH_STATUS {
    WAITING,
    RESCHEDULING,
    IN_PROGRESS,
    FAILED,
    MANUALLY_FAILED,
    TEAM_ONE_WIN,
    TEAM_TWO_WIN,
    DRAW
}

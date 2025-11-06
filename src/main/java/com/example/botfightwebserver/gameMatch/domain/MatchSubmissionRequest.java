package com.example.botfightwebserver.gameMatch.domain;

import lombok.Data;

@Data
public class MatchSubmissionRequest {
    private String team1Uuid;
    private String team2Uuid;
    private String submission1Uuid;
    private String submission2Uuid;
    private MATCH_REASON reason;
}

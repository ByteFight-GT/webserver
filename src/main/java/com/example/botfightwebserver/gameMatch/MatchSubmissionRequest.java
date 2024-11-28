package com.example.botfightwebserver.gameMatch;

import lombok.Data;

@Data
public class MatchSubmissionRequest {
    private Long team1Id;
    private Long team2Id;
    private Long submission1Id;
    private Long submission2Id;
    private MATCH_REASON reason;
    private String map;
}

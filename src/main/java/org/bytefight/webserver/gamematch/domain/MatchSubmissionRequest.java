package org.bytefight.webserver.gamematch.domain;

import lombok.Data;

@Data
public class MatchSubmissionRequest {
    private String team1Uuid;
    private String team2Uuid;
    private String submission1Uuid;
    private String submission2Uuid;
    private MatchReason reason;
}

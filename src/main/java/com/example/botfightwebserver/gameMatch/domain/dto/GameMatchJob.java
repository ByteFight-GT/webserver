package com.example.botfightwebserver.gameMatch.domain.dto;

import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.gameMatch.domain.MatchReason;
import com.example.botfightwebserver.submission.domain.STORAGE_SOURCE;
import com.example.botfightwebserver.submission.domain.Submission;

import java.io.Serializable;

public record GameMatchJob(String gameMatchUuid, String submission1Uuid, String submission2Uuid, STORAGE_SOURCE source1,
                           STORAGE_SOURCE source2, MatchReason reason, String map) implements Serializable {

    public static GameMatchJob from(GameMatch gameMatch) {
        Submission submission1 = gameMatch.getSubmissionOne();
        Submission submission2 = gameMatch.getSubmissionTwo();
        return new GameMatchJob(
            gameMatch.getUuid().toString(),
            submission1.getUuid().toString(),
            submission2.getUuid().toString(),
            submission1.getSource(),
            submission2.getSource(),
            gameMatch.getReason(),
            gameMatch.getMap()
        );
    }
}
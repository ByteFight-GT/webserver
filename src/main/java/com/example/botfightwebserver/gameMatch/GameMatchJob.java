package com.example.botfightwebserver.gameMatch;

import com.example.botfightwebserver.submission.STORAGE_SOURCE;
import com.example.botfightwebserver.submission.Submission;

import java.io.Serializable;

public record GameMatchJob(Long gameMatchId, String Submission1Path, String Submission2Path, STORAGE_SOURCE source1,
                           STORAGE_SOURCE source2, MATCH_REASON reason, String map) implements Serializable {

    public static GameMatchJob fromEntity(GameMatch gameMatch) {
        Submission submission1 = gameMatch.getSubmissionOne();
        Submission submission2 = gameMatch.getSubmissionTwo();
        return new GameMatchJob(
            gameMatch.getId(),
            submission1.getStoragePath(),
            submission2.getStoragePath(),
            submission1.getSource(),
            submission2.getSource(),
            gameMatch.getReason(),
            gameMatch.getMap()
        );
    }
}
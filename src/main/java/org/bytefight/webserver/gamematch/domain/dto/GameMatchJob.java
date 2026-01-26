package org.bytefight.webserver.gamematch.domain.dto;

import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.submission.domain.STORAGE_SOURCE;

import java.io.Serializable;

public record GameMatchJob(String gameMatchUuid, String submission1Uuid, String submission2Uuid, STORAGE_SOURCE source1,
                           STORAGE_SOURCE source2, MatchReason reason, String map) implements Serializable {

    public static GameMatchJob from(GameMatch gameMatch) {
//        Submission submission1 = gameMatch.getSubmissionOne();
//        Submission submission2 = gameMatch.getSubmissionTwo();
//        return new GameMatchJob(
//            gameMatch.getUuid().toString(),
//            submission1.getUuid().toString(),
//            submission2.getUuid().toString(),
//            submission1.getSource(),
//            submission2.getSource(),
//            gameMatch.getReason(),
//            gameMatch.getMap()
//        );
        return null;
    }
}
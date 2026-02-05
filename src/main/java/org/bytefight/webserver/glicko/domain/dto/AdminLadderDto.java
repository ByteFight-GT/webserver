package org.bytefight.webserver.glicko.domain.dto;

import org.bytefight.webserver.glicko.domain.Ladder;

public record AdminLadderDto(
        String id,
        Long competitionId,
        double glickoDefaultRating,
        double glickoDefaultRd,
        double glickoRdMax,
        Double glickoRdMin,
        double glickoPhiInflationPerDay,
        double glickoTau,
        double glickoSigmaDefault,
        Double glickoSigmaMin,
        Double glickoSigmaMax
) {
    public static AdminLadderDto from(Ladder ladder) {
        Long competitionId = ladder.getCompetition().getId();
        String ladderSlug = ladder.getLadder();
        return new AdminLadderDto(
                ladderSlug,
                competitionId,
                ladder.getGlickoDefaultRating(),
                ladder.getGlickoDefaultRd(),
                ladder.getGlickoRdMax(),
                ladder.getGlickoRdMin(),
                ladder.getGlickoPhiInflationPerDay(),
                ladder.getGlickoTau(),
                ladder.getGlickoSigmaDefault(),
                ladder.getGlickoSigmaMin(),
                ladder.getGlickoSigmaMax()
        );
    }
}

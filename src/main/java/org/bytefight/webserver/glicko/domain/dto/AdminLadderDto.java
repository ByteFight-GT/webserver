package org.bytefight.webserver.glicko.domain.dto;

import org.bytefight.webserver.glicko.domain.Ladder;

public record AdminLadderDto(
        Long id,
        String ladder,
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
        return new AdminLadderDto(
                ladder.getId(),
                ladder.getLadder(),
                ladder.getCompetition().getId(),
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

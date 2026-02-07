package org.bytefight.webserver.glicko.domain;

public class DefaultLadderSettings {
    public static Ladder baseline1500NoInflation() {
        return Ladder.builder()
                .glickoDefaultRating(1500.0)
                .glickoDefaultRd(350.0)
                .glickoRdMax(350.0)
                .glickoRdMin(30.0)
                .glickoPhiInflationPerDay(0.0)
                .glickoTau(0.5)
                .glickoSigmaDefault(0.06)
                .glickoSigmaMin(0.02)
                .glickoSigmaMax(0.03)
                .build();
    }
}

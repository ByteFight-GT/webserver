package com.example.botfightwebserver.glicko;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GlickoChanges {

    @Builder.Default
    private Double team1Change = 0.0;

    @Builder.Default
    private Double team2Change = 0.0;

    @Builder.Default
    private Double team1PhiChange = 0.0;

    @Builder.Default
    private Double team2PhiChange = 0.0;

    @Builder.Default
    private Double team1SigmaChange = 0.0;

    @Builder.Default
    private Double team2SigmaChange = 0.0;
}

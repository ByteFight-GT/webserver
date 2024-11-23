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
    private Double player1Change = 0.0;

    @Builder.Default
    private Double player2Change = 0.0;

    @Builder.Default
    private Double player1PhiChange = 0.0;

    @Builder.Default
    private Double player2PhiChange = 0.0;

    @Builder.Default
    private Double player1SigmaChange = 0.0;

    @Builder.Default
    private Double player2SigmaChange = 0.0;
}

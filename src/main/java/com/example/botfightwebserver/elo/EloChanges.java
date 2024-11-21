package com.example.botfightwebserver.elo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EloChanges {

    @Builder.Default
    private Double player1Change = 0.0;

    @Builder.Default
    private Double player2Change = 0.0;
}

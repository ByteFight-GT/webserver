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
    private Double player1Change;
    private Double player2Change;
}

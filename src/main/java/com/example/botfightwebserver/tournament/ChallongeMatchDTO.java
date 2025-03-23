package com.example.botfightwebserver.tournament;

import lombok.Builder;
import lombok.Data;

@Data  // or use @Getter @Setter
@Builder
public class ChallongeMatchDTO {
    private Integer round;
    private Long challongePlayer1Id;
    private Long challongePlayer2Id;
    private Long winnerId;
    private TOURNAMENT_SET_STATES state;
    private String matchId;

}
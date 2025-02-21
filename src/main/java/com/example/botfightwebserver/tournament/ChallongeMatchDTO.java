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
    private TOURNAMENT_MATCH_STATES state;
    private String matchId;

    // Optional: Add a converter method
    public TournamentGameMatch toTournamentGameMatch() {
        return TournamentGameMatch.builder()
            .round(round)
            .challongePlayer1Id(challongePlayer1Id)
            .challongePlayer2Id(challongePlayer1Id)
            .winnerId(winnerId)
            .state(state)
            .challongeMatchId(matchId)
            .build();
    }
}
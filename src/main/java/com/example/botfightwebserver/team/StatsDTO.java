package com.example.botfightwebserver.team;

import com.example.botfightwebserver.gameMatch.MATCH_REASON;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatsDTO {

    Integer numWins;
    Integer numLosses;
    Integer numDraws;
    MATCH_REASON matchReason;

}

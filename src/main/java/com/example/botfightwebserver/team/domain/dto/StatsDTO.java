package com.example.botfightwebserver.team.domain.dto;

import com.example.botfightwebserver.gameMatch.domain.MatchReason;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatsDTO {

    Integer numWins;
    Integer numLosses;
    Integer numDraws;
    MatchReason matchReason;

}

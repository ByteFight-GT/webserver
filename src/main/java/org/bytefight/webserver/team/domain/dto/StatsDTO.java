package org.bytefight.webserver.team.domain.dto;

import org.bytefight.webserver.gameMatch.domain.MatchReason;
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

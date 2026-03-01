package org.bytefight.webserver.team.domain.dto;

import lombok.Builder;
import lombok.Data;

import org.bytefight.webserver.gamematch.domain.MatchReason;

@Data
@Builder
public class StatsDTO {

  Integer numWins;
  Integer numLosses;
  Integer numDraws;
  MatchReason matchReason;
}

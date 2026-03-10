package org.bytefight.webserver.glicko.domain;

import org.bytefight.webserver.team.domain.Team;

public interface TeamStatsAggregate {
  Team getTeam();

  Integer getMatchesPlayed();

  Integer getWins();

  Integer getLosses();

  Integer getDraws();
}

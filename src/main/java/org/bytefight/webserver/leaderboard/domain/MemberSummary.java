package org.bytefight.webserver.leaderboard.domain;

public interface MemberSummary {
  String getTeamUuid();

  String getUuid();

  String getUsername();

  boolean getIsDev();
}

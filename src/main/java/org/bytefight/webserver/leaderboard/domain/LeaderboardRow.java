package org.bytefight.webserver.leaderboard.domain;

public interface LeaderboardRow {
  String getTeamUuid();

  String getTeamName();

  String getTeamQuote();

  String getTeamType();

  Double getGlickoRating();

  Integer getRank();

  boolean getTeamDisplayMembers();
}

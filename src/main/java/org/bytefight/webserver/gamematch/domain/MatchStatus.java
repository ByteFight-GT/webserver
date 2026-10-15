package org.bytefight.webserver.gamematch.domain;

public enum MatchStatus {
  created,
  scheduling,
  waiting,
  in_progress,
  failed,
  skipped,
  team_a_win,
  team_b_win,
  draw,
  submission_valid,
  submission_invalid,
}

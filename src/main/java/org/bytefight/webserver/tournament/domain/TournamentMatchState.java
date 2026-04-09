package org.bytefight.webserver.tournament.domain;

/** Lifecycle state of a tournament match node. */
public enum TournamentMatchState {
  PENDING,
  QUEUED,
  IN_PROGRESS,
  COMPLETE,
  SKIPPED
}

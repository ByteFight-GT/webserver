package com.example.botfightwebserver.tournament_cursor.domain;

/**
 * Lifecycle state of a tournament match node.
 */
public enum TournamentMatchState {
    PENDING,
    QUEUED,
    IN_PROGRESS,
    COMPLETE,
    SKIPPED
}

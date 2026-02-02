/**
 * Tournament Cursor module.
 *
 * Purpose:
 * - Provide a native, DB-backed double-elimination tournament system
 * - Scale to 300+ teams by using power-of-two brackets and byes
 * - Integrate with existing GameMatch queue and result processing
 *
 * High-level flow:
 * 1) Admin creates tournament (DRAFT)
 * 2) Admin enrolls teams (OPEN) -> TournamentEntry rows
 * 3) Admin starts tournament (IN_PROGRESS)
 *    - TournamentBracketBuilder builds bracket graph (TournamentMatch rows)
 *    - TournamentMatchScheduler queues matches using GameMatchService
 * 4) Game match results flow through GameMatchResultHandler
 *    - TournamentResultHandler applies win/loss and advances bracket
 * 5) Final winner completes tournament (COMPLETE)
 */
package com.example.botfightwebserver.tournament_cursor;

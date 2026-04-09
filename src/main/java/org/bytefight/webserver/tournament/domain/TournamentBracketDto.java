package org.bytefight.webserver.tournament.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Aggregated bracket payload: - tournament metadata - entries (seeded participants) - matches
 * (bracket graph)
 */
@Getter
@Builder
public class TournamentBracketDto {
  private final TournamentDto tournament;
  private final List<TournamentEntryDto> entries;
  private final List<TournamentMatchDto> matches;
}

package com.example.botfightwebserver.tournament_cursor.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Read-only tournament metadata for API responses.
 */
@Getter
@Builder
public class TournamentDto {
    private final String uuid;
    private final String name;
    private final TournamentStatus status;
    private final Integer maxTeams;
    private final Integer bracketSize;
    private final LocalDateTime createdAt;
    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;

    public static TournamentDto from(Tournament tournament) {
        return TournamentDto.builder()
                .uuid(tournament.getUuid().toString())
                .name(tournament.getName())
                .status(tournament.getStatus())
                .maxTeams(tournament.getMaxTeams())
                .bracketSize(tournament.getBracketSize())
                .createdAt(tournament.getCreatedAt())
                .startedAt(tournament.getStartedAt())
                .finishedAt(tournament.getFinishedAt())
                .build();
    }
}

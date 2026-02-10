package org.bytefight.webserver.glicko.domain.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.bytefight.webserver.glicko.domain.TeamGlickoHistory;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class TeamGlickoHistoryDto {
    @NotNull String teamUuid;
    @NotNull String competitionSlug;
    @NotNull String ladder;
    @NotNull Instant recordedAt;

    double oldGlicko;
    double newGlicko;

    public static TeamGlickoHistoryDto from(TeamGlickoHistory history) {
        return TeamGlickoHistoryDto.builder()
                .teamUuid(history.getTeam().getUuid().toString())
                .competitionSlug(history.getCompetition().getSlug())
                .ladder(history.getLadder())
                .recordedAt(history.getCreatedAt()) // Inherited from BaseEntity
                .oldGlicko(history.getOldGlicko())
                .newGlicko(history.getNewGlicko())
                .build();
    }
}
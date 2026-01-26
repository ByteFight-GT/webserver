package org.bytefight.webserver.leaderboard;

import org.bytefight.webserver.team.domain.TeamType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class LeaderboardDTO {
    @NotNull String teamUuid;
    @NotNull String teamName;
    @NotNull String quote;
    @NotNull LocalDateTime createdAt;
    @NotNull TeamType type;

    @NotNull int rank;
    @NotNull double glicko;
    List<String> members;

}

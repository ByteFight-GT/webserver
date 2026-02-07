package org.bytefight.webserver.leaderboard.domain;

import org.bytefight.webserver.team.domain.TeamType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class LeaderboardDto {
    @NotNull String teamUuid;
    @NotNull String teamName;
    @NotNull String teamQuote;
    @NotNull TeamType type;

    @NotNull double glicko;
    Double rank;
    List<String> members;
}

package com.example.botfightwebserver.leaderboard;

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

    int rank;
    double glicko;
    List<String> members;

}

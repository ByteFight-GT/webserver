package com.example.botfightwebserver.leaderboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LeaderboardDTO {
    private Long teamId;
    private int rank;
    private double glicko;
    private String teamName;
    private LocalDateTime createdAt;
    private List<String> members;
    private String quote;
}

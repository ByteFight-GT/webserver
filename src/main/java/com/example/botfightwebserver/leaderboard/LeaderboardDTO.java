package com.example.botfightwebserver.leaderboard;

import com.google.api.client.util.DateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LeaderboardDTO {
    private Long id;
    private int rank;
    private String teamName;
    private DateTime createdAt;
    private String[] members;
    private String quote;
}

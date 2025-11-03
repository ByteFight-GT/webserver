package com.example.botfightwebserver.gameMatchLogs;

import com.example.botfightwebserver.gameMatch.domain.GameMatchDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameMatchLogDTO {
    @NotNull private GameMatchDTO gameMatchDTO;

    @NotNull private String matchLog;

    private Double team1GlickoChange;

    private Double team2GlickoChange;

    public static GameMatchLogDTO from(GameMatchLog gameMatchLog) {
        return GameMatchLogDTO.builder()
            .gameMatchDTO(GameMatchDTO.fromEntity(gameMatchLog.getGameMatch()))
            .matchLog(gameMatchLog.getMatchLog())
            .team1GlickoChange(gameMatchLog.getTeam1GlickoChange())
            .team2GlickoChange(gameMatchLog.getTeam2GlickoChange())
            .build();
    }
}

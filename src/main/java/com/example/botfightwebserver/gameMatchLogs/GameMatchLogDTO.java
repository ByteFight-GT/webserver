package com.example.botfightwebserver.gameMatchLogs;

import com.example.botfightwebserver.gameMatch.GameMatchDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameMatchLogDTO {

    private Long id;

    private GameMatchDTO gameMatchDTO;

    private String matchLog;

    private Double team1GlickoChange;

    private Double team2GlickoChange;

    public static GameMatchLogDTO fromEntity(GameMatchLog gameMatchLog) {
        GameMatchLogDTO gameMatchLogDTO = GameMatchLogDTO.builder()
            .id(gameMatchLog.getId())
            .gameMatchDTO(GameMatchDTO.fromEntity(gameMatchLog.getGameMatch()))
            .matchLog(gameMatchLog.getMatchLog())
            .team1GlickoChange(gameMatchLog.getTeam1GlickoChange())
            .team2GlickoChange(gameMatchLog.getTeam2GlickoChange())
            .build();
        return gameMatchLogDTO;
    }
}

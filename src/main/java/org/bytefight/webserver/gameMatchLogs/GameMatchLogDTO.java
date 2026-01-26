package org.bytefight.webserver.gameMatchLogs;

import org.bytefight.webserver.gamematch.domain.dto.GameMatchDto;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameMatchLogDTO {
    @NotNull private GameMatchDto gameMatchDTO;

    @NotNull private String matchLog;

    private Double team1GlickoChange;

    private Double team2GlickoChange;

    public static GameMatchLogDTO from(GameMatchLog gameMatchLog) {
        return GameMatchLogDTO.builder()
            .gameMatchDTO(GameMatchDto.fromEntity(gameMatchLog.getGameMatch()))
            .matchLog(gameMatchLog.getMatchLog())
            .team1GlickoChange(gameMatchLog.getTeam1GlickoChange())
            .team2GlickoChange(gameMatchLog.getTeam2GlickoChange())
            .build();
    }
}

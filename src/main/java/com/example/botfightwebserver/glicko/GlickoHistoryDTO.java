package com.example.botfightwebserver.glicko;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GlickoHistoryDTO {
    private Long teamId;
    private Double glicko;
    private LocalDateTime saveDate;

    public static GlickoHistoryDTO fromEntity(GlickoHistory glickoHistory) {
        return GlickoHistoryDTO.builder()
            .teamId(glickoHistory.getTeamId())
            .glicko(glickoHistory.getGlicko())
            .saveDate(glickoHistory.getSaveDate())
            .build();
    }
}
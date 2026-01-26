package org.bytefight.webserver.glicko;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GlickoHistoryDTO {
    private String teamUuid;
    private Double glicko;
    private LocalDateTime saveDate;

    public static GlickoHistoryDTO fromEntity(GlickoHistory glickoHistory) {
        return GlickoHistoryDTO.builder()
            .teamUuid(glickoHistory.getTeam().getUuid().toString())
            .glicko(glickoHistory.getGlicko())
            .saveDate(glickoHistory.getSaveDate())
            .build();
    }
}
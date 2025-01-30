package com.example.botfightwebserver.glicko;

import com.example.botfightwebserver.config.ClockConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GlickoHistoryService {

    private final  GlickoHistoryRepository glickoHistoryRepository;
    private final ClockConfig clockConfig;

    public GlickoHistory save(long teamId, Double glicko) {
        GlickoHistory glickoHistory = GlickoHistory.builder()
            .teamId(teamId)
            .glicko(glicko)
            .saveDate(LocalDateTime.now(clockConfig.clock()))
            .build();
        glickoHistoryRepository.save(glickoHistory);
        return glickoHistory;
    }

    public List<GlickoHistory> getTeamHistory(long teamId) {
        return glickoHistoryRepository.findByTeamId(teamId);
    }
}

package com.example.botfightwebserver.glicko;

import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.team.domain.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GlickoHistoryService {

    private final  GlickoHistoryRepository glickoHistoryRepository;
    private final ClockConfig clockConfig;
    private final PlayerService playerService;

    public GlickoHistory save(Team team, Double glicko) {
        GlickoHistory glickoHistory = GlickoHistory.builder()
            .team(team)
            .glicko(glicko)
            .saveDate(LocalDateTime.now(clockConfig.clock()))
            .build();
        glickoHistoryRepository.save(glickoHistory);
        return glickoHistory;
    }

    public List<GlickoHistory> getTeamHistory(String teamUuid) {
        return glickoHistoryRepository.findByTeamUuid(UUID.fromString(teamUuid));
    }
}

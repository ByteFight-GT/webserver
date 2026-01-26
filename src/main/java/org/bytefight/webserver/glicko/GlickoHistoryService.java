package org.bytefight.webserver.glicko;

import org.bytefight.webserver.config.ClockConfig;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.team.domain.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GlickoHistoryService {

    private final  GlickoHistoryRepository glickoHistoryRepository;
    private final ClockConfig clockConfig;
    private final PlayerService playerService;

    public GlickoHistory save(Team team, GameMatch gameMatch) {
//        GlickoHistory glickoHistory = new GlickoHistory(
//                null,
//                team,
//                gameMatch,
//                team.getGlicko(),
//                LocalDateTime.now(clockConfig.clock())
//        );
//
//        glickoHistoryRepository.save(glickoHistory);
        return null;
    }

    public List<GlickoHistory> getTeamHistory(String teamUuid) {
        return glickoHistoryRepository.findByTeamUuid(UUID.fromString(teamUuid));
    }
}

package org.bytefight.webserver.leaderboard.application;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.glicko.domain.Ladder;
import org.bytefight.webserver.glicko.infra.TeamStatsRepository;
import org.bytefight.webserver.leaderboard.domain.LeaderboardRow;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderboardService {
    private final TeamStatsRepository teamStatsRepository;

    public List<LeaderboardRow> getFullLeaderboardByCompetitionAndLadder(Competition competition, String ladder) {
        return teamStatsRepository.findLeaderboardRowsByCompetitionAndLadder(competition, ladder);
    }
}

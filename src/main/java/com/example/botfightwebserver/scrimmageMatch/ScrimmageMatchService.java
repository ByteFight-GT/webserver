package com.example.botfightwebserver.scrimmageMatch;

import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.gameMatch.application.GameMatchService;
import com.example.botfightwebserver.gameMatch.domain.MatchStatus;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.application.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScrimmageMatchService {

    private final ScrimmageMatchRepository scrimmageMatchDataRepository;
    private final TeamService teamService;
    private final GameMatchService gameMatchService;

    private final static Long ALLOWED_IN_PROGRESS_SCRIMMAGES = 10L;

    public Long remainingAllowedScrimmages(Long teamId) {
        return ALLOWED_IN_PROGRESS_SCRIMMAGES - getInProgressScrimmages(teamId);
    }

    public ScrimmageMatch createScrimmageMatchData(GameMatch match, Team inititorTeam) {
        ScrimmageMatch scrimmageMatchData = ScrimmageMatch.builder().match(match).initiatorTeam(inititorTeam).build();
        return scrimmageMatchDataRepository.save(scrimmageMatchData);
    }

    public Long getInProgressScrimmages(Long teamId) {
        Team team = teamService.getTeamById(teamId);
        return scrimmageMatchDataRepository.countByMatchStatusAndInitiatorTeam(MatchStatus.WAITING, team);
    }

}

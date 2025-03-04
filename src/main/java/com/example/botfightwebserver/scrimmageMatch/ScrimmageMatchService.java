package com.example.botfightwebserver.scrimmageMatch;

import com.example.botfightwebserver.gameMatch.GameMatch;
import com.example.botfightwebserver.gameMatch.GameMatchService;
import com.example.botfightwebserver.gameMatch.MATCH_STATUS;
import com.example.botfightwebserver.team.Team;
import com.example.botfightwebserver.team.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScrimmageMatchService {

    private final ScrimmageMatchRepository scrimmageMatchDataRepository;
    private final TeamService teamService;
    private final GameMatchService gameMatchService;

    private final static Long ALLOWED_IN_PROGRESS_SCRIMMAGES = 10;

    public Long remainingAllowedScrimmages(Long teamId) {
        return ALLOWED_IN_PROGRESS_SCRIMMAGES - getInProgressScrimmages(teamId);
    }

    public ScrimmageMatch createScrimmageMatchData(GameMatch match, Team inititorTeam) {
        ScrimmageMatch scrimmageMatchData = ScrimmageMatch.builder().match(match).initiatorTeam(inititorTeam).build();
        return scrimmageMatchDataRepository.save(scrimmageMatchData);
    }

    public Long getInProgressScrimmages(Long teamId) {
        Team team = teamService.getReferenceById(teamId);
        return scrimmageMatchDataRepository.countByMatchStatusAndInitiatorTeam(MATCH_STATUS.WAITING, team);
    }

}

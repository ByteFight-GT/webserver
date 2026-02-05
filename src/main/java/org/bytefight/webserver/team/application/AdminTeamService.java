package org.bytefight.webserver.team.application;

import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AdminTeamService {
    private final TeamRepository teamRepository;

    public AdminTeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public Page<Team> listTeams(Long competitionId, boolean isDeleted, Pageable pageable) {
        if (competitionId != null) {
            return teamRepository.findByCompetitionIdAndIsDeleted(competitionId, isDeleted, pageable);
        }
        return teamRepository.findByIsDeleted(isDeleted, pageable);
    }
}

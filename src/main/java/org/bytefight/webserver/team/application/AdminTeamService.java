package org.bytefight.webserver.team.application;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.dto.AdminCreateTeamDto;
import org.bytefight.webserver.team.domain.dto.TeamSettingsDto;
import org.bytefight.webserver.team.domain.dto.AdminUpdateTeamDto;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminTeamService {
    private final CompetitionRepository competitionRepository;
    private final TeamRepository teamRepository;
    private final TeamService teamService;

    public AdminTeamService(CompetitionRepository competitionRepository, TeamRepository teamRepository, TeamService teamService) {
        this.competitionRepository = competitionRepository;
        this.teamRepository = teamRepository;
        this.teamService = teamService;
    }

    public Page<Team> listTeams(Long competitionId, boolean isDeleted, Pageable pageable) {
        if (competitionId != null) {
            return teamRepository.findByCompetitionIdAndIsDeleted(competitionId, isDeleted, pageable);
        }
        return teamRepository.findByIsDeleted(isDeleted, pageable);
    }

    public Team createTeam(AdminCreateTeamDto input) {
        Competition competition = competitionRepository.findById(input.getCompetitionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found"));
        Boolean displayMembers = input.getDisplayMembers() != null ? input.getDisplayMembers() : Boolean.FALSE;
        TeamSettingsDto settings = new TeamSettingsDto(input.getName(), input.getQuote(), displayMembers);
        return teamService.createTeam(competition, settings);
    }

    public Team updateTeam(Long id, AdminUpdateTeamDto input) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));

        TeamSettingsDto settings = new TeamSettingsDto(
                input.name(),
                input.quote(),
                input.displayMembers()
        );

        teamService.editTeam(team, settings);

        if (input.type() != null) {
            team.setType(input.type());
            teamRepository.save(team);
        }

        return team;
    }

    public Team getTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
    }
}

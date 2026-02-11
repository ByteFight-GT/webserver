package org.bytefight.webserver.competition.application;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.domain.dto.AdminCreateCompetitionDto;
import org.bytefight.webserver.competition.domain.dto.AdminUpdateCompetitionDto;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminCompetitionService {
    private final CompetitionRepository competitionRepository;

    public AdminCompetitionService(CompetitionRepository competitionRepository) {
        this.competitionRepository = competitionRepository;
    }

    public Page<Competition> listCompetitions(Pageable pageable) {
        return competitionRepository.findAll(pageable);
    }

    public Competition getCompetition(Long id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found"));
    }

    public Competition createCompetition(AdminCreateCompetitionDto input) {
        String slug = input.getSlug();
        if (competitionRepository.findBySlug(slug).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Competition slug already exists");
        }

        Competition competition = new Competition();
        competition.setSlug(slug);
        competition.setName(input.getName());
        competition.setActive(false);

        return competitionRepository.save(competition);
    }

    public Competition updateCompetition(Long id, AdminUpdateCompetitionDto input) {
        Competition competition = competitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found"));

        if (input.getName() != null) {
            competition.setName(input.getName());
        }
        if (input.getDescription() != null) {
            competition.setDescription(input.getDescription());
        }
        if (input.getIsActive() != null) {
            competition.setActive(input.getIsActive());
        }
        if (input.getIsWhitelisted() != null) {
            competition.setWhitelisted(input.getIsWhitelisted());
        }
        if (input.getAllowNewSubmission() != null) {
            competition.setAllowNewSubmission(input.getAllowNewSubmission());
        }
        if (input.getAllowSetSubmission() != null) {
            competition.setAllowSetSubmission(input.getAllowSetSubmission());
        }
        if (input.getAllowCreateTeam() != null) {
            competition.setAllowCreateTeam(input.getAllowCreateTeam());
        }
        if (input.getAllowJoinTeam() != null) {
            competition.setAllowJoinTeam(input.getAllowJoinTeam());
        }
        if (input.getAllowLeaveTeam() != null) {
            competition.setAllowLeaveTeam(input.getAllowLeaveTeam());
        }
        if (input.getMaxPlayersPerTeam() != null) {
            competition.setMaxPlayersPerTeam(input.getMaxPlayersPerTeam());
        }

        return competitionRepository.save(competition);
    }
}

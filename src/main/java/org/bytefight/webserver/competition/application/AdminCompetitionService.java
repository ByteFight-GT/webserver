package org.bytefight.webserver.competition.application;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.domain.dto.AdminCreateCompetitionDto;
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
}

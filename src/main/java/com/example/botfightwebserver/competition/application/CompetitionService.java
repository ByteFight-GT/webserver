package com.example.botfightwebserver.competition.application;

import com.example.botfightwebserver.competition.domain.Competition;
import com.example.botfightwebserver.competition.infra.CompetitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompetitionService {
    private final CompetitionRepository competitionRepository;

    public Optional<Competition> getCompetitionBySlug(String slug) {
        String normalizedSlug = slug.trim().toLowerCase();
        return competitionRepository.findBySlug(normalizedSlug);
    }
}

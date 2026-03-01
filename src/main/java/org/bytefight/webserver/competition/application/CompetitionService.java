package org.bytefight.webserver.competition.application;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompetitionService {
  private final CompetitionRepository competitionRepository;

  public List<Competition> getAllCompetitions() {
    return competitionRepository.findAll();
  }

  public Optional<Competition> getCompetitionBySlug(String slug) {
    String normalizedSlug = slug.trim().toLowerCase();
    return competitionRepository.findBySlug(normalizedSlug);
  }
}

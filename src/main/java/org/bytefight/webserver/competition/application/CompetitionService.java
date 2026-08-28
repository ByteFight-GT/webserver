package org.bytefight.webserver.competition.application;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.bytefight.webserver.team.infra.TeamMemberRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompetitionService {
  private final CompetitionRepository competitionRepository;
  private final CompetitionAccessGuard accessGuard;
  private final TeamMemberRepository teamMemberRepository;

  public List<Competition> getAllCompetitions() {
    Sort sort = Sort.by(Sort.Order.desc("createdAt"));
    return accessGuard.canSeeInternal()
        ? competitionRepository.findAll(sort)
        : competitionRepository.findAllByInternalFalse(sort);
  }

  public Optional<Competition> getCompetitionBySlug(String slug) {
    String normalizedSlug = slug.trim().toLowerCase();
    return competitionRepository.findBySlug(normalizedSlug).filter(accessGuard::canAccess);
  }
}

package org.bytefight.webserver.competition.application;

import lombok.RequiredArgsConstructor;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.domain.dto.AdminCreateCompetitionDto;
import org.bytefight.webserver.gamematch.domain.DefaultLadders;
import org.bytefight.webserver.competition.domain.dto.AdminUpdateCompetitionDto;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.bytefight.webserver.ladder.application.LadderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@Service
public class AdminCompetitionService {
  private final LadderService ladderService;
  private final CompetitionRepository competitionRepository;

  public Page<Competition> listCompetitions(Pageable pageable) {
    return competitionRepository.findAll(pageable);
  }

  public Competition getCompetition(Long id) {
    return competitionRepository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found"));
  }

  public Competition createCompetition(AdminCreateCompetitionDto input) {
    String slug = input.getSlug().toLowerCase().trim();
    if (competitionRepository.findBySlug(slug).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Competition slug already exists");
    }

    Competition competition = new Competition();
    competition.setSlug(slug);
    competition.setName(input.getName());
    competition.setActive(false);

    //    competition.setTeamSubmissionStorageSize(200 * 1000 * 1000);

    competition = competitionRepository.save(competition);

    ladderService.createLadder(
        competition, "validation", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    // Scrim matches are unrated practice against TA bots, but game_matches has a composite FK on
    // (competition_id, ladder), so the scrim ladder must exist as a row. Glicko config is unused.
    ladderService.createLadder(
        competition, DefaultLadders.SCRIM, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

    return competition;
  }

  public Competition updateCompetition(Long id, AdminUpdateCompetitionDto input) {
    Competition competition =
        competitionRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found"));

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
    if (input.getAllowEditTeamName() != null) {
      competition.setAllowEditTeamName(input.getAllowEditTeamName());
    }
    if (input.getTeamSubmissionStorageSize() != null) {
      competition.setTeamSubmissionStorageSize(input.getTeamSubmissionStorageSize());
    }
    if (input.getAllowCreateUserMatch() != null) {
      competition.setAllowCreateUserMatch(input.getAllowCreateUserMatch());
    }
    if (input.getSettings() != null) {
      competition.setSettings(input.getSettings());
    }
    //    competition.setTeamSubmissionStorageSize(200 * 1000 * 1000);

    return competitionRepository.save(competition);
  }
}

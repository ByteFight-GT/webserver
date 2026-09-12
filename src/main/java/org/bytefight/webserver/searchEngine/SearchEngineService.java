package org.bytefight.webserver.searchEngine;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bytefight.webserver.competition.application.CompetitionAccessGuard;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.TeamMember;
import org.bytefight.webserver.team.domain.dto.PublicTeamDto;
import org.bytefight.webserver.team.infra.TeamMemberRepository;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchEngineService {
  private final CompetitionRepository competitionRepository;
  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final CompetitionAccessGuard accessGuard;

  @Transactional
  public Page<PublicTeamDto> searchRecruitingTeams(
      String searchTerm, String competitionSlug, Pageable pageable) {
    Competition competition =
        competitionRepository
            .findBySlug(competitionSlug)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found"));
    accessGuard.requireAccess(competition);

    String normalizedSearchTerm = searchTerm.trim();
    log.info(
        "Searching recruiting teams in competition {} for: {}",
        competitionSlug,
        normalizedSearchTerm);

    Page<Team> teams =
        teamRepository.searchRecruitingTeams(
            competition, normalizedSearchTerm, competition.getMaxPlayersPerTeam(), pageable);
    List<Team> teamList = teams.getContent();
    Map<Long, List<Player>> membersByTeamId = loadMembersByTeamId(teamList);
    List<PublicTeamDto> data =
        teamList.stream()
            .map(
                team ->
                    PublicTeamDto.fromRecruitingTeam(
                        team, membersByTeamId.getOrDefault(team.getId(), List.of())))
            .toList();
    return new PageImpl<>(data, teams.getPageable(), teams.getTotalElements());
  }

  private Map<Long, List<Player>> loadMembersByTeamId(List<Team> teams) {
    Map<Long, List<Player>> membersByTeamId = new HashMap<>();
    if (teams.isEmpty()) {
      return membersByTeamId;
    }

    for (TeamMember member : teamMemberRepository.findByTeamIn(teams)) {
      membersByTeamId
          .computeIfAbsent(member.getTeam().getId(), ignored -> new ArrayList<>())
          .add(member.getPlayer());
    }
    return membersByTeamId;
  }
}

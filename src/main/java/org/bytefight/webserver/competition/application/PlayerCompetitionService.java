package org.bytefight.webserver.competition.application;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.bytefight.webserver.competition.domain.dto.PlayerCompetitionDto;
import org.bytefight.webserver.glicko.infra.TeamStatsRepository;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.TeamMember;
import org.bytefight.webserver.team.infra.TeamMemberRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerCompetitionService {
  private final TeamMemberRepository teamMemberRepository;
  private final TeamStatsRepository teamStatsRepository;

  public List<PlayerCompetitionDto> getPlayerCompetitions(Player player) {
    List<TeamMember> memberships = teamMemberRepository.findByPlayerAndTeamIsDeletedFalse(player);

    return memberships.stream()
        .map(membership -> {
          Team team = membership.getTeam();
          var competition = team.getCompetition();

          List<PlayerCompetitionDto.MemberDto> memberDtos =
              teamMemberRepository.findByTeam(team).stream()
                  .map(TeamMember::getPlayer)
                  .map(p -> new PlayerCompetitionDto.MemberDto(
                      p.getUser().getUuid().toString(), p.getUsername()))
                  .toList();

          List<PlayerCompetitionDto.RankingDto> rankingDtos =
              teamStatsRepository
                  .findTeamRanksByCompetition(competition.getId(), team.getId())
                  .stream()
                  .map(r -> new PlayerCompetitionDto.RankingDto(r.getLadder(), r.getRank()))
                  .toList();

          return new PlayerCompetitionDto(
              competition.getSlug(),
              competition.getName(),
              team.getName(),
              team.getUuid().toString(),
              memberDtos,
              rankingDtos);
        })
        .toList();
  }
}

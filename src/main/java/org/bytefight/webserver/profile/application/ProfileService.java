package org.bytefight.webserver.profile.application;

import lombok.RequiredArgsConstructor;
import org.bytefight.webserver.competition.application.CompetitionAccessGuard;
import org.bytefight.webserver.profile.domain.dto.PlayerCompetitionDto;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.TeamMember;
import org.bytefight.webserver.team.infra.TeamMemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final CompetitionAccessGuard competitionAccessGuard;
    private final TeamMemberRepository teamMemberRepository;

    public List<PlayerCompetitionDto> getCompetitionsByPlayer(Player player) {
        List<TeamMember> memberships = teamMemberRepository.findByPlayerAndTeamDeletedAtNull(player);

        return memberships.stream()
                .map(membership -> {
                    Team team = membership.getTeam();
                    var competition = team.getCompetition();

                    if (!competitionAccessGuard.canAccess(competition)) {
                        return null;
                    }

                    List<PlayerCompetitionDto.PlayerCompetitionMemberDto> memberDtos =
                            teamMemberRepository.findByTeam(team).stream()
                                    .map(TeamMember::getPlayer)
                                    .map(p -> new PlayerCompetitionDto.PlayerCompetitionMemberDto(
                                            p.getUser().getUuid().toString(), p.getUsername()))
                                    .toList();

                    return new PlayerCompetitionDto(
                            competition.getSlug(),
                            competition.getName(),
                            team.getName(),
                            team.getUuid().toString(),
                            team.isDisplayMembers() ? memberDtos : null,
                            team.getCachedLeaderboardRank());
                })
                .filter(Objects::nonNull)
                .toList();
    }
}

package org.bytefight.webserver.leaderboard.application;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.glicko.infra.TeamStatsRepository;
import org.bytefight.webserver.leaderboard.domain.LeaderboardDto;
import org.bytefight.webserver.leaderboard.domain.LeaderboardRow;
import org.bytefight.webserver.leaderboard.domain.MemberSummary;
import org.bytefight.webserver.leaderboard.domain.MemberSummaryDto;
import org.bytefight.webserver.team.domain.TeamType;
import org.bytefight.webserver.team.infra.TeamMemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {
    private final TeamStatsRepository teamStatsRepository;
    private final TeamMemberRepository teamMemberRepository;

    public LeaderboardService(TeamStatsRepository teamStatsRepository, TeamMemberRepository teamMemberRepository) {
        this.teamStatsRepository = teamStatsRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    public List<LeaderboardDto> getFullLeaderboardByCompetitionAndLadder(Competition competition, String ladder) {
        List<LeaderboardRow> rows = teamStatsRepository.findLeaderboardRowsByCompetitionAndLadder(competition, ladder);
        if (rows.isEmpty()) {
            return List.of();
        }

        List<String> teamUuids = rows.stream()
                .map(LeaderboardRow::getTeamUuid)
                .toList();

        List<MemberSummary> memberSummaries = teamMemberRepository.findMemberSummariesByTeamUuids(teamUuids);
        Map<String, List<MemberSummaryDto>> membersByTeamUuid = memberSummaries.stream()
                .collect(Collectors.groupingBy(
                        MemberSummary::getTeamUuid,
                        Collectors.mapping(summary -> new MemberSummaryDto(summary.getUuid(), summary.getUsername()), Collectors.toList())
                ));

        return rows.stream()
                .map(row -> new LeaderboardDto(
                        row.getTeamUuid(),
                        row.getTeamName(),
                        row.getTeamQuote(),
                        TeamType.valueOf(row.getTeamType()),
                        row.getGlickoRating(),
                        row.getRank(),
                        membersByTeamUuid.getOrDefault(row.getTeamUuid(), List.of())
                ))
                .toList();
    }
}

package org.bytefight.webserver.team.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bytefight.webserver.competition.application.AdminCompetitionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Team Member (Admin)")
@RequestMapping("/api/v1/admin/competition-player-count")
@PreAuthorize("hasRole('ADMIN')")
@RestController
public class AdminCompetitionPlayerCount {
    private final TeamMemberRepository teamMemberRepository;
    private final AdminCompetitionService adminCompetitionService;

    public AdminCompetitionPlayerCount(
            TeamMemberRepository teamMemberRepository,
            AdminCompetitionService adminCompetitionService
    ) {
        this.teamMemberRepository = teamMemberRepository;
        this.adminCompetitionService = adminCompetitionService;
    }

    @GetMapping("/{competitionId}")
    @Operation(
            operationId = "adminGetCompetitionPlayerCount",
            summary = "REST endpoint to get player count for a competition"
    )
    public CompetitionPlayerCountDto getCompetitionPlayerCount(@PathVariable Long competitionId) {
        adminCompetitionService.getCompetition(competitionId);
        long playerCount = teamMemberRepository.countPlayersByCompetitionId(competitionId);
        return new CompetitionPlayerCountDto(competitionId, playerCount);
    }

    public record CompetitionPlayerCountDto(Long competitionId, Long playerCount) {
    }
}

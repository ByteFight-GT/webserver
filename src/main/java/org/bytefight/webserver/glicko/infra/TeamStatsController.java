package org.bytefight.webserver.glicko.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.bytefight.webserver.glicko.application.TeamStatsService;
import org.bytefight.webserver.glicko.domain.TeamStats;
import org.bytefight.webserver.glicko.domain.dto.TeamStatsDto;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Tag(name = "Team Stats (Public)", description = "Public read-only team endpoints")
@RestController
@RequestMapping("/api/v1/public/team-stats")
@RequiredArgsConstructor
public class TeamStatsController {
    private final TeamStatsService teamStatsService;
    private final TeamService teamService;


    @GetMapping("/{teamUuid}/{ladderSlug}")
    @Operation(
            summary = "Get a teams stats"
    )
    public ResponseEntity<TeamStatsDto> getTeamStatsByTeamUuidAndLadder(@PathVariable UUID teamUuid, @PathVariable String ladderSlug){
        Team team = teamService.getTeamByUuid(teamUuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        TeamStats teamStats = teamStatsService.getTeamStatsCreateIfNotExist(team, ladderSlug);

        return ResponseEntity.ok(TeamStatsDto.from(teamStats));
    }

}

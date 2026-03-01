package org.bytefight.webserver.glicko.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.bytefight.webserver.glicko.application.GlickoService;
import org.bytefight.webserver.glicko.domain.TeamGlickoHistory;
import org.bytefight.webserver.glicko.domain.dto.TeamGlickoHistoryDto;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Team Glicko History (Public)", description = "Public read-only team endpoints")
@RestController
@RequestMapping("/api/v1/public/glicko-history")
@RequiredArgsConstructor
public class TeamGlickoHistoryController {
  private final GlickoService glickoService;
  private final TeamService teamService;

  @GetMapping("/{teamUuid}/{ladderSlug}")
  @Operation(summary = "Get a teams Glicko History")
  public ResponseEntity<List<TeamGlickoHistoryDto>> getGlickoHistoryByTeamUuidAndLadder(
      @PathVariable UUID teamUuid, @PathVariable String ladderSlug) {
    Team team =
        teamService
            .getTeamByUuid(teamUuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    List<TeamGlickoHistory> teamGlickoHistories =
        glickoService.getTeamGlickoHistoryByTeamAndLadder(team, ladderSlug);

    return ResponseEntity.ok(TeamGlickoHistoryDto.listFrom(teamGlickoHistories));
  }
}

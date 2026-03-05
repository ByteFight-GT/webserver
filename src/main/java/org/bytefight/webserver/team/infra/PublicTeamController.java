package org.bytefight.webserver.team.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.bytefight.webserver.config.ClockConfig;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.dto.PublicTeamDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Team (Public)", description = "Public read-only team endpoints")
@RestController
@RequestMapping("/api/v1/public/team")
@RequiredArgsConstructor
public class PublicTeamController {
  private final TeamService teamService;
  private final ClockConfig clockConfig;

  @GetMapping("/{uuid}")
  @Operation(summary = "Get a public team by UUID")
  public ResponseEntity<PublicTeamDto> getPublicTeamByUuid(@PathVariable UUID uuid) {
    Team team =
        teamService
            .getTeamByUuid(uuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<Player> members = teamService.getPlayersForTeam(team);
        return ResponseEntity.ok(PublicTeamDto.from(team, members));
    }
  }

  @GetMapping("/teams-with-submission")
  public ResponseEntity<Integer> countTeamsWithSubmission() {
    return ResponseEntity.ok((teamService.countTeamsWithSubmission()));
  }
}

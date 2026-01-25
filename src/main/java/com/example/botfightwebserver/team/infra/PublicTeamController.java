package com.example.botfightwebserver.team.infra;

import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.glicko.GlickoCalculator;
import com.example.botfightwebserver.glicko.GlickoHistoryDTO;
import com.example.botfightwebserver.glicko.GlickoHistoryService;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.dto.PublicTeamDto;
import com.example.botfightwebserver.team.domain.Team;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "Teams (Public)", description = "Public read-only team endpoints")
@RestController
@RequestMapping("/api/v1/public/team")
@RequiredArgsConstructor
public class PublicTeamController {
    private final TeamService teamService;
    private final GlickoHistoryService glickoHistoryService;
    private final ClockConfig clockConfig;

    @GetMapping("/{uuid}")
    @Operation(
            summary = "Get a public team by UUID"
    )
    public ResponseEntity<PublicTeamDto> getPublicTeamByUuid(@PathVariable UUID uuid) {
        Team team = teamService.getTeamByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if(!team.isDisplayMembers()) {
            return ResponseEntity.ok(PublicTeamDto.from(team, null));
        } else {
            List<Player> members = teamService.getPlayersForTeam(team);
            return ResponseEntity.ok(PublicTeamDto.from(team, members));
        }
    }

    @GetMapping("/teams-with-submission")
    public ResponseEntity<Integer> countTeamsWithSubmission() {
        return ResponseEntity.ok((teamService.countTeamsWithSubmission()));
    }
}

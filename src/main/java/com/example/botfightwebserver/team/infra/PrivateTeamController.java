package com.example.botfightwebserver.team.infra;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.glicko.GlickoHistoryService;
import com.example.botfightwebserver.permissions.application.PermissionsService;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.dto.TeamSettingsDto;
import com.example.botfightwebserver.team.domain.dto.SelfTeamDto;
import com.example.botfightwebserver.team.domain.Team;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.*;

@Tag(name = "Teams (Private)")
@RestController
@RequestMapping("/api/v1/team")
@RequiredArgsConstructor
@Transactional
public class PrivateTeamController {

    private final TeamService teamService;
    private final PlayerService playerService;
    private final Clock clock;
    private final GlickoHistoryService glickoHistoryService;
    private final ClockConfig clockConfig;
    private final PermissionsService permissionsService;

    @PostMapping("/{uuid}")
    @Operation(
            operationId = "editTeam",
            summary = "Update a team's settings"
    )
    public ResponseEntity<SelfTeamDto> editTeam(
            @AuthenticationPrincipal User user,
            @PathVariable UUID uuid,
            @RequestBody TeamSettingsDto teamSettingsDto
    ) {
//        permissionsService.validateAllowUpdateTeam();
        Player player = playerService.getPlayer(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        Team team = teamService.getTeamByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));

        if (!teamService.isMember(team, player)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this team");
        }

        teamService.editTeam(team, teamSettingsDto);

        List<Player> members = teamService.getPlayersForTeam(team);
        return ResponseEntity.ok(SelfTeamDto.from(team, members));
    }

//    @PostMapping("/set-submission")
//    public ResponseEntity<Void> setCurrentSubmission(@AuthenticationPrincipal User user, @RequestParam String submissionUuid) {
//        permissionsService.validateAllowSetSubmission();
//        Player player = playerService.getPlayer(user);
//        Long teamId = player.getTeam().getId();
//        teamService.setCurrentSubmission(teamId, submissionUuid);
//        return ResponseEntity.ok().build();
//    }
}

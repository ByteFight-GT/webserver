package com.example.botfightwebserver.team.infra;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.glicko.GlickoHistoryService;
import com.example.botfightwebserver.permissions.application.PermissionsService;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.TeamSettingsDto;
import com.example.botfightwebserver.team.domain.SelfTeamDto;
import com.example.botfightwebserver.team.domain.Team;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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

    @Operation(
            operationId = "getCurrentTeam",
            summary = "Get current user's team"
    )
    @GetMapping("/my-team")
    public ResponseEntity<SelfTeamDto> getCurrentTeam(@AuthenticationPrincipal User user) {
        Player player = playerService.getPlayer(user);
        if (!player.isHasTeam() || player.getTeam() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Optional<SelfTeamDto> selfTeamDtoOptional = teamService.getSelfTeamDtoByUuid(player.getTeam().getUuid().toString());
        return selfTeamDtoOptional.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/new")
    public ResponseEntity<SelfTeamDto> createTeam(@AuthenticationPrincipal User user, @RequestBody TeamSettingsDto teamSettingsDto) {
        permissionsService.validateAllowCreateTeam();
        Player player = playerService.getPlayer(user);
        if(player.isHasTeam()) throw new IllegalArgumentException("You're already on a team!");
        Team team = teamService.createTeam(user, teamSettingsDto);
        playerService.setPlayerTeam(user.getUuid(), team);
        return ResponseEntity.ok(SelfTeamDto.from(team, -1));
    }

    @PostMapping("/edit")
    public ResponseEntity<Void> editTeam(@AuthenticationPrincipal User user, @RequestBody TeamSettingsDto edit) {
        permissionsService.validateAllowUpdateTeam();
        boolean isAvailable = !teamService.isNameExist(edit.getName());
        String currentTeamName = playerService.getPlayer(user.getUuid()).getTeam().getName();
        if (!isAvailable && !currentTeamName.equals(edit.getName())) {
            throw new IllegalArgumentException("Name is Already Taken.");
        }

        Player player = playerService.getPlayer(user.getUuid());
        Team team = player.getTeam();

        teamService.editTeam(team.getId(), edit);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/set-submission")
    public ResponseEntity<Void> setCurrentSubmission(@AuthenticationPrincipal User user, @RequestParam String submissionUuid) {
        permissionsService.validateAllowSetSubmission();
        Player player = playerService.getPlayer(user);
        Long teamId = player.getTeam().getId();
        teamService.setCurrentSubmission(teamId, submissionUuid);
        return ResponseEntity.ok().build();
    }
}

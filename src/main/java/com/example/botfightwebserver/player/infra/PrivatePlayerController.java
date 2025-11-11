package com.example.botfightwebserver.player.infra;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.player.domain.PlayerUsername;
import com.example.botfightwebserver.player.domain.PublicPlayerDto;
import com.example.botfightwebserver.player.domain.SelfPlayerDto;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.application.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Player (Private)")
@RestController
@RequestMapping("/api/v1/player")
@RequiredArgsConstructor
@Validated
public class PrivatePlayerController {
    private final PlayerService playerService;
    private final TeamService teamService;

    @PostMapping("/join-team")
    public ResponseEntity<PublicPlayerDto> joinTeam(@AuthenticationPrincipal User user, @RequestParam String teamCode) {
        Team team = teamService.findTeamByCode(teamCode);
        if (!teamService.isTeamJoinable(team)) {
            throw new IllegalArgumentException("Team " + team.getName() + " is not joinable");
        }
        Player player = playerService.getPlayer(user);
        if(player.isHasTeam()) throw new IllegalArgumentException("You're already on a team!");

        player = playerService.setPlayerTeam(user.getUuid(), team);
        teamService.incrementTeamMembers(team.getId());
        return ResponseEntity.ok(PublicPlayerDto.from(player));
    }

    @GetMapping("/player")
    public ResponseEntity<PublicPlayerDto> getPlayerById(@RequestParam Long id) {
        return ResponseEntity.ok(PublicPlayerDto.from(playerService.getPlayer(id)));
    }

    @Operation(
            operationId = "getCurrentPlayer",
            summary = "Get current player profile"
    )
    @GetMapping("/me")
    public ResponseEntity<SelfPlayerDto> getCurrentPlayer(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(SelfPlayerDto.from(playerService.getPlayer(user)));
    }

    @PostMapping("/name")
    public ResponseEntity<Map<String, String>> updateName(@AuthenticationPrincipal User user, @RequestParam @PlayerUsername String name) {
        name = name.trim();
        boolean isAvailable = !playerService.isUsernameExist(name);
        if (!isAvailable) {
            throw new IllegalArgumentException("Name " + name + " is not available");
        }
        Player player = playerService.getPlayer(user);
        if (player.getName().equals(name)) {
            return ResponseEntity.ok(Collections.singletonMap("setName", "Name Cannot Be Same"));
        }
        playerService.setName(player.getId(), name);
        return ResponseEntity.ok(Collections.singletonMap("setName", "Succesfully updated!"));
    }

    @PostMapping("/leave-team")
    public ResponseEntity<Void> leaveTeam(@AuthenticationPrincipal User user) {
        Player player = playerService.getPlayer(user);
        Team oldTeam = playerService.leaveTeam(player);
        teamService.decrementTeamMembers(oldTeam.getId());
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleException(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}

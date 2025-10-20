package com.example.botfightwebserver.player.infra;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.player.domain.PlayerUsername;
import com.example.botfightwebserver.player.domain.PublicPlayerDto;
import com.example.botfightwebserver.player.domain.SelfPlayerDto;
import com.example.botfightwebserver.team.Team;
import com.example.botfightwebserver.team.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/player")
@RequiredArgsConstructor
@Validated
public class PlayerController {
    private final PlayerService playerService;
    private final TeamService teamService;

    @PostMapping("/team")
    public ResponseEntity<PublicPlayerDto> assignTeam(@RequestParam Long teamId) {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Team team = teamService.getReferenceById(teamId);
        if (!teamService.isTeamJoinable(team)) {
            throw new IllegalArgumentException("Team " + teamId + " is not joinable");
        }
        Player player = playerService.setPlayerTeam(UUID.fromString(authId), teamId);
        teamService.incrementTeamMembers(teamId);
        return ResponseEntity.ok(PublicPlayerDto.from(player
        ));
    }

    @PostMapping("/join-team")
    public ResponseEntity<PublicPlayerDto> joinTeam(@AuthenticationPrincipal User user, @RequestParam String teamCode) {
        Team team = teamService.findTeamByCode(teamCode);
        if (!teamService.isTeamJoinable(team)) {
            throw new IllegalArgumentException("Team " + team.getName() + " is not joinable");
        }
        Player player = playerService.setPlayerTeam(user.getUuid(), team.getId());
        teamService.incrementTeamMembers(team.getId());
        return ResponseEntity.ok(PublicPlayerDto.from(player));
    }

    @GetMapping("/player")
    public ResponseEntity<PublicPlayerDto> getPlayerById(@RequestParam Long id) {
        return ResponseEntity.ok(PublicPlayerDto.from(playerService.getPlayer(id)));
    }

    @GetMapping("/me")
    public ResponseEntity<SelfPlayerDto> getMe(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(SelfPlayerDto.from(playerService.getPlayer(user)));
    }

    @GetMapping("/public/check-username/{username}")
    public ResponseEntity<Map<String, Boolean>> checkUsernameAvailability(@PathVariable String username) {
        boolean isAvailable = !playerService.isUsernameExist(username);
        return ResponseEntity.ok(Collections.singletonMap("available", isAvailable));
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

    @GetMapping("/team-id")
    public ResponseEntity<List<PublicPlayerDto>> getPlayersByTeamId(@RequestParam Long teamId) {
        return ResponseEntity.ok(playerService.getPlayersByTeam(teamId).stream().map(PublicPlayerDto::from).toList());
    }

    @PostMapping("/leave-team")
    public ResponseEntity<Void> leaveTeam(@AuthenticationPrincipal User user) {
        Long oldTeamId = playerService.leaveTeam(user.getUuid());
        teamService.decrementTeamMembers(oldTeamId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/public/check-email/{email}")
    public ResponseEntity<Map<String, Boolean>> checkEmailAvailability(@PathVariable String email) {
        boolean isAvailable = !playerService.isEmailExist(email);
        return ResponseEntity.ok(Collections.singletonMap("available", isAvailable));
    }

    @GetMapping("/public/count")
    public ResponseEntity<Long> getPlayerCount() {
        return ResponseEntity.ok(playerService.getNumberPlayers());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleException(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}

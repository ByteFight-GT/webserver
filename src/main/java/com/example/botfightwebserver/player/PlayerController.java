package com.example.botfightwebserver.player;

import com.example.botfightwebserver.team.Team;
import com.example.botfightwebserver.team.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class PlayerController {

    private final PlayerService playerService;
    private final TeamService teamService;

    @GetMapping("/players")
    public ResponseEntity<List<PlayerDTO>> getAllPlayers() {
        return ResponseEntity.ok(playerService.getPlayers().stream()
            .map(PlayerDTO::fromEntity)
            .toList());
    }

    @PostMapping("/create")
    public ResponseEntity<PlayerDTO> createPlayer(
        @RequestParam String name,
        @RequestParam String email,
        @RequestParam(required = false) Long teamId) {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        return ResponseEntity.ok(PlayerDTO.fromEntity(
            playerService.createPlayer(name, email, UUID.fromString(authId), teamId)));
    }

    @PostMapping("/team")
    public ResponseEntity<PlayerDTO> assignTeam(@RequestParam Long teamId) {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Team team = teamService.getReferenceById(teamId);
        if (!teamService.isTeamJoinable(team)) {
            throw new IllegalArgumentException("Team " + teamId + " is not joinable");
        }
        Player player = playerService.setPlayerTeam(UUID.fromString(authId), teamId);
        teamService.incrementTeamMembers(teamId);
        return ResponseEntity.ok(PlayerDTO.fromEntity(player
            ));
    }

    @PostMapping("/join-team")
    public ResponseEntity<PlayerDTO> joinTeam(@RequestParam String teamCode) {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Team team = teamService.findTeamByCode(teamCode);
        if (!teamService.isTeamJoinable(team)) {
            throw new IllegalArgumentException("Team " + team.getName() + " is not joinable");
        }
        Player player = playerService.setPlayerTeam(UUID.fromString(authId), team.getId());
        teamService.incrementTeamMembers(team.getId());
        return ResponseEntity.ok(PlayerDTO.fromEntity(player));
    }

    @GetMapping("/player")
    public ResponseEntity<PlayerDTO> getPlayerById(@RequestParam Long id) {
        return ResponseEntity.ok(PlayerDTO.fromEntity(playerService.getPlayer(id)));
    }

    @GetMapping("/player-auth")
    public ResponseEntity<PlayerDTO> getPlayerByAuthId(@RequestParam String authId) {
        return ResponseEntity.ok(PlayerDTO.fromEntity(playerService.getPlayer(UUID.fromString(authId))));
    }


    @GetMapping("/me")
    public ResponseEntity<PlayerDTO> getMe() {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        return ResponseEntity.ok(PlayerDTO.fromEntity(playerService.getPlayer(UUID.fromString(authId))));
    }


    public boolean hasAccess(UUID requestedAuthId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = (String) auth.getPrincipal();
        return currentUserId.equals(requestedAuthId.toString());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleException(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @GetMapping("/public/check-username/{username}")
    public ResponseEntity<Map<String, Boolean>> checkUsernameAvailability(@PathVariable String username) {
        boolean isAvailable = !playerService.isUsernameExist(username);
        return ResponseEntity.ok(Collections.singletonMap("available", isAvailable));
    }

    @PostMapping("/name")
    public ResponseEntity<Map<String, String>> updateName(@RequestParam String name) {
        boolean isAvailable = !playerService.isUsernameExist(name);
        if (!isAvailable) {
            return ResponseEntity.ok(Collections.singletonMap("setName", "Name is Already Taken."));
        }
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Player player = playerService.getPlayer(UUID.fromString(authId));
        if (player.getName().equals(name)) {
            return ResponseEntity.ok(Collections.singletonMap("setName", "Name Cannot Be Same"));
        }
        playerService.setName(player.getId(), name);
        return ResponseEntity.ok(Collections.singletonMap("setName", "Succesfully updated!"));
    }

    @PostMapping("/leave-team")
    public ResponseEntity<Void> leaveTeam() {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Long oldTeamId = playerService.leaveTeam(UUID.fromString(authId));
        teamService.decrementTeamMembers(oldTeamId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/public/check-email/{email}")
    public ResponseEntity<Map<String, Boolean>> checkEmailAvailability(@PathVariable String email) {
        boolean isAvailable = ! playerService.isEmailExist(email);
        return ResponseEntity.ok(Collections.singletonMap("available", isAvailable));
    }
}

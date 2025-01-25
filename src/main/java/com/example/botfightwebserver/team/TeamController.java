package com.example.botfightwebserver.team;

import com.example.botfightwebserver.glicko.GlickoHistoryDTO;
import com.example.botfightwebserver.player.Player;
import com.example.botfightwebserver.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final TeamAuditService teamAuditService;
    private final PlayerService playerService;

    @GetMapping("/teams")
    public List<TeamDTO> getTeams() {
        return teamService.getTeams().stream().map(TeamDTO::fromEntity).toList();
    }

    @GetMapping("/team")
    public ResponseEntity<TeamDTO> getTeam(@RequestParam Long teamId) {
        return ResponseEntity.ok(teamService.getDTOById(teamId));
    }

    @GetMapping("/my-team")
    public ResponseEntity<TeamDTO> getTeam() {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Player player = playerService.getPlayer(UUID.fromString(authId));
        return ResponseEntity.ok(teamService.getDTOById(player.getTeamId()));
    }

    @PostMapping
    public ResponseEntity<TeamDTO> createTeam(@RequestParam String name) {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Team team = teamService.createTeam(name);
        playerService.setPlayerTeam(UUID.fromString(authId), team.getId());
        return ResponseEntity.ok(TeamDTO.fromEntity(team));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @PostMapping("/quote")
    public ResponseEntity<String> setQuote(@RequestParam Long teamId,@RequestParam String quote) {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Player player = playerService.getPlayer(UUID.fromString(authId));
        if (!player.getTeamId().equals(teamId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        teamService.setQuote(teamId, quote);
        return ResponseEntity.ok(quote);
    }

    @GetMapping("/glicko-history")
    public ResponseEntity<List<GlickoHistoryDTO>> getGlickoHistory(@RequestParam Long teamId) {
        return ResponseEntity.ok(teamAuditService.getGlickoHistory(teamId));
    }

}

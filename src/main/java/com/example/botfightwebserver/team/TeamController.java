package com.example.botfightwebserver.team;

import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.glicko.GlickoHistoryDTO;
import com.example.botfightwebserver.glicko.GlickoHistoryService;
import com.example.botfightwebserver.player.Player;
import com.example.botfightwebserver.player.PlayerService;
import io.opencensus.stats.Stats;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/team")
@RequiredArgsConstructor
@Transactional
public class TeamController {

    private final TeamService teamService;
    private final PlayerService playerService;
    private final Clock clock;
    private final GlickoHistoryService glickoHistoryService;
    private final ClockConfig clockConfig;

    @GetMapping("/teams")
    public List<TeamDTO> getTeams() {
        return teamService.getTeams().stream().map(TeamDTO::fromEntity).toList();
    }

    @GetMapping("/public/team")
    public ResponseEntity<TeamDTO> getTeam(@RequestParam Long teamId) {
        return ResponseEntity.ok(teamService.getDTOById(teamId));
    }

    @GetMapping("/my-team")
    public ResponseEntity<TeamDTO> getTeam() {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Player player = playerService.getPlayer(UUID.fromString(authId));
        if (!player.isHasTeam()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(teamService.getDTOById(player.getTeamId()));
    }

    @PostMapping
    public ResponseEntity<TeamDTO> createTeam(@RequestParam String name) {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Team team = teamService.createTeam(name);
        playerService.setPlayerTeam(UUID.fromString(authId), team.getId());
        return ResponseEntity.ok(TeamDTO.fromEntity(team));
    }

    @PostMapping("/name")
    public ResponseEntity<Map<String, String>> setName(@RequestParam Long teamId, @RequestParam String name) {
        boolean isAvailable = !teamService.isNameExist(name);
        if (!isAvailable) {
            return ResponseEntity.ok(Collections.singletonMap("setName", "Name is Already Taken."));
        }
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Player player = playerService.getPlayer(UUID.fromString(authId));
        if (!player.getTeamId().equals(teamId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        teamService.setName(teamId, name);
        return ResponseEntity.ok(Collections.singletonMap("setName", "Succesfully updated!"));
    }

    @PostMapping("/quote")
    public ResponseEntity<String> setQuote(@RequestParam Long teamId, @RequestParam String quote) {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Player player = playerService.getPlayer(UUID.fromString(authId));
        if (!player.getTeamId().equals(teamId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        teamService.setQuote(teamId, quote);
        return ResponseEntity.ok(quote);
    }

    @GetMapping("/public/glicko-history")
    public ResponseEntity<List<GlickoHistoryDTO>> getGlickoHistory(@RequestParam Long teamId) {
        Team team = teamService.getReferenceById(teamId);
        List<GlickoHistoryDTO> glickoHistories = new ArrayList<>(
            glickoHistoryService.getTeamHistory(teamId).stream().map(GlickoHistoryDTO::fromEntity).toList());
        glickoHistories.add(GlickoHistoryDTO.builder().teamId(teamId).glicko(team.getGlicko())
            .saveDate(LocalDateTime.now(clockConfig.clock())).build());
        return ResponseEntity.ok(glickoHistories);
    }

    @GetMapping("/my-glicko-history")
    public ResponseEntity<List<GlickoHistoryDTO>> getMyGlickoHistory() {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Long teamId = playerService.getTeamFromUUID(UUID.fromString(authId));
        Team team = teamService.getReferenceById(teamId);
        List<GlickoHistoryDTO> glickoHistories = new ArrayList<>(
            glickoHistoryService.getTeamHistory(teamId).stream().map(GlickoHistoryDTO::fromEntity).toList());
        glickoHistories.add(GlickoHistoryDTO.builder().teamId(teamId).glicko(team.getGlicko())
            .saveDate(LocalDateTime.now(clockConfig.clock())).build());
        return ResponseEntity.ok(glickoHistories);
    }

    @GetMapping("/public/teams-with-submission")
    public ResponseEntity<Integer> countTeamsWithSubmission() {
           return ResponseEntity.ok((teamService.countTeamsWithSubmission()));
    }

    @PostMapping("/set-submission")
    public ResponseEntity<Void> setCurrentSubmission(@RequestParam Long submissionId) {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Player player = playerService.getPlayer(UUID.fromString(authId));
        Long teamId = player.getTeamId();
        teamService.setCurrentSubmission(teamId, submissionId);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleException(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}

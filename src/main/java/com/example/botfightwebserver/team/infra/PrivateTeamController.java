package com.example.botfightwebserver.team.infra;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.glicko.GlickoHistoryDTO;
import com.example.botfightwebserver.glicko.GlickoHistoryService;
import com.example.botfightwebserver.permissions.PermissionsService;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.SelfTeamDto;
import com.example.botfightwebserver.team.domain.Team;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
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
        if (!player.isHasTeam()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(SelfTeamDto.from(teamService.getTeamById(player.getTeamId())));
    }

    @PostMapping
    public ResponseEntity<SelfTeamDto> createTeam(@AuthenticationPrincipal User user, @RequestParam String name) {
        permissionsService.validateAllowCreateTeam();
        Team team = teamService.createTeam(name);
        playerService.setPlayerTeam(user.getUuid(), team.getId());
        return ResponseEntity.ok(SelfTeamDto.from(team));
    }

    @PostMapping("/name")
    public ResponseEntity<Map<String, String>> setName(@RequestParam Long teamId, @RequestParam String name) {
        permissionsService.validateAllowUpdateTeam();
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
        permissionsService.validateAllowUpdateTeam();
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Player player = playerService.getPlayer(UUID.fromString(authId));
        if (!player.getTeamId().equals(teamId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        teamService.setQuote(teamId, quote);
        return ResponseEntity.ok(quote);
    }

    @GetMapping("/my-glicko-history")
    public ResponseEntity<List<GlickoHistoryDTO>> getMyGlickoHistory(@AuthenticationPrincipal User user) {
        Long teamId = playerService.getTeamFromUUID(user.getUuid());
        Team team = teamService.getTeamById(teamId);
        List<GlickoHistoryDTO> glickoHistories = new ArrayList<>(
            glickoHistoryService.getTeamHistory(teamId).stream().map(GlickoHistoryDTO::fromEntity).toList());
        glickoHistories.add(GlickoHistoryDTO.builder().teamId(teamId).glicko(team.getGlicko())
            .saveDate(LocalDateTime.now(clockConfig.clock())).build());
        return ResponseEntity.ok(glickoHistories);
    }

    @PostMapping("/set-submission")
    public ResponseEntity<Void> setCurrentSubmission(@RequestParam Long submissionId) {
        permissionsService.validateAllowSetSubmission();
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

package com.example.botfightwebserver.competition.infra;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.competition.application.CompetitionService;
import com.example.botfightwebserver.competition.domain.Competition;
import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.domain.dto.PublicTeamDto;
import com.example.botfightwebserver.team.domain.dto.SelfTeamDto;
import com.example.botfightwebserver.team.domain.dto.TeamSettingsDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Tag(name = "Competitions (Private)", description = """
        """)
@RestController
@RequestMapping("/api/v1/competition")
@RequiredArgsConstructor
public class PrivateCompetitionController {
    private final CompetitionService competitionService;
    private final TeamService teamService;
    private final PlayerService playerService;

    @PostMapping("/{competitionSlug}/teams")
    @Transactional
    public ResponseEntity<SelfTeamDto> createCompetitionTeam(
            @AuthenticationPrincipal User user,
            @PathVariable String competitionSlug,
            @RequestBody TeamSettingsDto teamSettingsDto
    ) {
        Competition competition = competitionService.getCompetitionBySlug(competitionSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition with slug not found"));

        Player player = playerService.getPlayer(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        Team team = teamService.createTeam(competition, teamSettingsDto);

        teamService.joinTeam(player, team);

        return ResponseEntity.ok(SelfTeamDto.from(team));
    }
}

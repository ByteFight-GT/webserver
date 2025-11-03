package com.example.botfightwebserver.team.infra;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.AdminCreateTeamDto;
import com.example.botfightwebserver.team.domain.SelfTeamDto;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.domain.TeamSettingsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/team")
public class AdminTeamController {
    private final TeamService teamService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SelfTeamDto> adminCreateTeam(@RequestBody AdminCreateTeamDto adminCreateTeamDto) {
        Team team = teamService.adminCreateTeam(adminCreateTeamDto);
        return ResponseEntity.ok(SelfTeamDto.from(team, -1));
    }
}

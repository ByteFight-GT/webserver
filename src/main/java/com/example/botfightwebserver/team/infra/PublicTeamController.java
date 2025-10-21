package com.example.botfightwebserver.team.infra;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.glicko.GlickoHistoryDTO;
import com.example.botfightwebserver.glicko.GlickoHistoryService;
import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.PublicTeamDto;
import com.example.botfightwebserver.team.domain.SelfTeamDto;
import com.example.botfightwebserver.team.domain.Team;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Tag(name = "Teams (Public)", description = "Public read-only team endpoints")
@RestController
@RequestMapping("/api/v1/public/team")
@RequiredArgsConstructor
public class PublicTeamController {
    private final PlayerService playerService;
    private final TeamService teamService;
    private final GlickoHistoryService glickoHistoryService;
    private final ClockConfig clockConfig;

    @GetMapping("/all")
    public List<PublicTeamDto> getTeams() {
        return teamService.getTeams().stream().map(PublicTeamDto::from).toList();
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<? extends PublicTeamDto> getTeam(@AuthenticationPrincipal User user, @PathVariable String uuid) {
        Optional<Team> teamOptional = teamService.getTeamByUuid(uuid);

        if(teamOptional.isEmpty()) return ResponseEntity.notFound().build();

        Team team = teamOptional.get();

        if(user != null) {
            Player player = playerService.getPlayer(user.getUuid());
            if(player.getTeam().equals(team)) {
                return ResponseEntity.ok(SelfTeamDto.from(team));
            }
        }

        return ResponseEntity.ok(PublicTeamDto.from(team));
    }

    @GetMapping("/glicko-history/{uuid}")
    public ResponseEntity<List<GlickoHistoryDTO>> getGlickoHistory(@PathVariable String uuid) {
        Optional<Team> teamOptional = teamService.getTeamByUuid(uuid);

        if (teamOptional.isEmpty()) return ResponseEntity.notFound().build();

        Team team = teamOptional.get();

        List<GlickoHistoryDTO> glickoHistories = new ArrayList<>(
                glickoHistoryService.getTeamHistory(uuid).stream().map(GlickoHistoryDTO::fromEntity).toList());
        glickoHistories.add(GlickoHistoryDTO.builder().teamUuid(team.getUuid().toString()).glicko(team.getGlicko())
                .saveDate(LocalDateTime.now(clockConfig.clock())).build());
        return ResponseEntity.ok(glickoHistories);
    }

    @GetMapping("/teams-with-submission")
    public ResponseEntity<Integer> countTeamsWithSubmission() {
        return ResponseEntity.ok((teamService.countTeamsWithSubmission()));
    }
}

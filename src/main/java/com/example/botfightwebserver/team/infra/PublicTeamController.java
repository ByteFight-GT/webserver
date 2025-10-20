package com.example.botfightwebserver.team.infra;

import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.glicko.GlickoHistoryDTO;
import com.example.botfightwebserver.glicko.GlickoHistoryService;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.PublicTeamDto;
import com.example.botfightwebserver.team.domain.Team;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/team")
@RequiredArgsConstructor
public class PublicTeamController {
    private final TeamService teamService;
    private final GlickoHistoryService glickoHistoryService;
    private final ClockConfig clockConfig;

    @GetMapping("/all")
    public List<PublicTeamDto> getTeams() {
        return teamService.getTeams().stream().map(PublicTeamDto::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicTeamDto> getTeam(@PathVariable Long id) {
        return ResponseEntity.ok(PublicTeamDto.from(teamService.getTeamById(id)));
    }

    @GetMapping("/glicko-history/{id}")
    public ResponseEntity<List<GlickoHistoryDTO>> getGlickoHistory(@PathVariable Long id) {
        Team team = teamService.getTeamById(id);
        List<GlickoHistoryDTO> glickoHistories = new ArrayList<>(
                glickoHistoryService.getTeamHistory(id).stream().map(GlickoHistoryDTO::fromEntity).toList());
        glickoHistories.add(GlickoHistoryDTO.builder().teamId(id).glicko(team.getGlicko())
                .saveDate(LocalDateTime.now(clockConfig.clock())).build());
        return ResponseEntity.ok(glickoHistories);
    }

    @GetMapping("/teams-with-submission")
    public ResponseEntity<Integer> countTeamsWithSubmission() {
        return ResponseEntity.ok((teamService.countTeamsWithSubmission()));
    }
}

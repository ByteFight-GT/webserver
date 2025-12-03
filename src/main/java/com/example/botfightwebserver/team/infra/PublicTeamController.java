package com.example.botfightwebserver.team.infra;

import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.glicko.GlickoCalculator;
import com.example.botfightwebserver.glicko.GlickoHistoryDTO;
import com.example.botfightwebserver.glicko.GlickoHistoryService;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.PublicTeamDto;
import com.example.botfightwebserver.team.domain.Team;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    private final TeamService teamService;
    private final GlickoHistoryService glickoHistoryService;
    private final ClockConfig clockConfig;

    @GetMapping("/{uuid}")
    public ResponseEntity<PublicTeamDto> getTeam(@PathVariable String uuid) {
        Optional<PublicTeamDto> dtoOptional = teamService.getPublicTeamDtoByUuid(uuid);

        return dtoOptional.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/glicko-history/{uuid}")
    public ResponseEntity<List<GlickoHistoryDTO>> getGlickoHistory(@PathVariable String uuid) {
        Optional<Team> teamOptional = teamService.getTeamByUuid(uuid);

        if (teamOptional.isEmpty()) return ResponseEntity.notFound().build();

        Team team = teamOptional.get();

        List<GlickoHistoryDTO> glickoHistories = new ArrayList<>(
                glickoHistoryService.getTeamHistory(uuid).stream().map(GlickoHistoryDTO::fromEntity).toList());
        glickoHistories.add(0,
                GlickoHistoryDTO.builder()
                        .teamUuid(team.getUuid().toString())
                        .glicko(GlickoCalculator.MU) // initial rating
                        .saveDate(team.getCreationDateTime()
                ).build()
        );
        return ResponseEntity.ok(glickoHistories);
    }

    @GetMapping("/teams-with-submission")
    public ResponseEntity<Integer> countTeamsWithSubmission() {
        return ResponseEntity.ok((teamService.countTeamsWithSubmission()));
    }
}

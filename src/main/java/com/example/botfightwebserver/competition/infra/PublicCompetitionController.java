package com.example.botfightwebserver.competition.infra;

import com.example.botfightwebserver.competition.application.CompetitionService;
import com.example.botfightwebserver.competition.domain.Competition;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.domain.dto.PublicTeamDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Tag(name = "Competitions (Public)", description = """
        Public, read-only endpoins for browsing competitions and their related data,
        including teams, standings, and basic metadata.
        """)
@RestController
@RequestMapping("/api/v1/public/competition")
@RequiredArgsConstructor
public class PublicCompetitionController {
    private final CompetitionService competitionService;
    private final TeamService teamService;

    @GetMapping("/{competitionSlug}/{uuid}")
    public ResponseEntity<PublicTeamDto> getTeamByCompetition(@PathVariable String competitionSlug, @PathVariable String uuidString) {
        UUID uuid = UUID.fromString(uuidString);
        Competition competition = competitionService.getCompetitionBySlug(competitionSlug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Team team = teamService.getTeamByCompetitionAndUuid(competition, uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return ResponseEntity.ok(PublicTeamDto.from(team, -1));
    }
}

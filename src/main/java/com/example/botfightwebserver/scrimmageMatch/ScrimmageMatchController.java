package com.example.botfightwebserver.scrimmageMatch;

import com.example.botfightwebserver.gameMatch.GameMatch;
import com.example.botfightwebserver.gameMatch.GameMatchService;
import com.example.botfightwebserver.gameMatch.MatchSubmissionRequest;
import com.example.botfightwebserver.player.PlayerService;
import com.example.botfightwebserver.team.Team;
import com.example.botfightwebserver.team.TeamService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scrimmage")
@RequiredArgsConstructor
@Transactional
public class ScrimmageMatchController {

    private final ScrimmageMatchService scrimmageMatchService;
    private final GameMatchService gameMatchService;
    private final PlayerService playerService;
    private final TeamService teamService;


    @PostMapping("/create")
    public ResponseEntity<ScrimmageMatch> createScrimmageMatch(@RequestBody MatchSubmissionRequest request, @RequestParam Integer number) {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Long teamId = playerService.getTeamFromUUID(UUID.fromString(authId));
        Team team = teamService.getReferenceById(teamId);

        Long remainingAllowedScrimmages = scrimmageMatchService.remainingAllowedScrimmages(teamId);
        if (number > remainingAllowedScrimmages) {
            throw new IllegalArgumentException("Your team only has " + remainingAllowedScrimmages + " scrimmages allowed at this time");
        }

        GameMatch match = gameMatchService.submitGameMatch(
            request.getTeam1Id(),
            request.getTeam2Id(),
            request.getSubmission1Id(),
            request.getSubmission2Id(),
            request.getReason(),
            request.getMap());
        return ResponseEntity.ok(scrimmageMatchService.createScrimmageMatchData(match, team));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleException(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}

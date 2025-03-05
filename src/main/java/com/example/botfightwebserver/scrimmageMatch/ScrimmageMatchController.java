package com.example.botfightwebserver.scrimmageMatch;

import com.example.botfightwebserver.gameMatch.GameMatch;
import com.example.botfightwebserver.gameMatch.GameMatchService;
import com.example.botfightwebserver.gameMatch.MATCH_REASON;
import com.example.botfightwebserver.player.PlayerService;
import com.example.botfightwebserver.submission.Submission;
import com.example.botfightwebserver.team.Team;
import com.example.botfightwebserver.team.TeamService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    public ResponseEntity<List<ScrimmageMatchDTO>> createScrimmageMatch(@RequestParam Integer number,
                                                                        @RequestParam String map,
                                                                        @RequestParam(required = false) Long team2Id) {

        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Long team1Id = playerService.getTeamFromUUID(UUID.fromString(authId));
        if (team2Id == null) {
            // scrimmage against self
            team2Id = team1Id;
        }
        Team team = teamService.getReferenceById(team1Id);
        Optional<Submission> team1CurrentSubmission = teamService.getCurrentSubmission(team1Id);
        Optional<Submission> team2CurrentSubmission = teamService.getCurrentSubmission(team2Id);

        if (!team1CurrentSubmission.isPresent() || !team2CurrentSubmission.isPresent()) {
            throw new IllegalArgumentException("Both teams must have submission");
        }

        Long remainingAllowedScrimmages = scrimmageMatchService.remainingAllowedScrimmages(team1Id);
        if (number > remainingAllowedScrimmages) {
            throw new IllegalArgumentException("Your team only has " + remainingAllowedScrimmages + " scrimmages allowed at this time");
        }

        List<ScrimmageMatchDTO> scrimmages = new ArrayList<ScrimmageMatchDTO>();
        for (int i = 0; i < number; i++) {
            GameMatch match = gameMatchService.submitGameMatch(
                team1Id,
                team2Id,
                team1CurrentSubmission.get().getId(),
                team2CurrentSubmission.get().getId(),
                MATCH_REASON.SCRIMMAGE,
                map);
            ScrimmageMatchDTO scrimmageMatchDTO = ScrimmageMatchDTO.fromEntity(scrimmageMatchService.createScrimmageMatchData(match, team));
            scrimmages.add(scrimmageMatchDTO);
        }
        return ResponseEntity.ok(scrimmages);
    }

    @GetMapping("/remaining-scrimmages")
    public ResponseEntity<Long> getRemainingScrimmages() {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Long teamId = playerService.getTeamFromUUID(UUID.fromString(authId));
        return ResponseEntity.ok(scrimmageMatchService.remainingAllowedScrimmages(teamId));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleException(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}

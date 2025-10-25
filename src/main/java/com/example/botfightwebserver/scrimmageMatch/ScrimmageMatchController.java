package com.example.botfightwebserver.scrimmageMatch;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.gameMatch.application.GameMatchService;
import com.example.botfightwebserver.gameMatch.domain.MATCH_REASON;
import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.submission.domain.Submission;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.application.TeamService;
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
    public ResponseEntity<List<ScrimmageMatchDTO>> createScrimmageMatch(@AuthenticationPrincipal User user,
                                                                        @RequestParam Integer number,
                                                                        @RequestParam String map,
                                                                        @RequestParam(required = false) String team2Uuid) {

        Team selfTeam = playerService.getTeamFromUUID(user.getUuid());
        if (team2Uuid == null) {
            team2Uuid = selfTeam.getUuid().toString();
        }

        Optional<Team> opponentTeamOpt = teamService.getTeamByUuid(team2Uuid);
        if(opponentTeamOpt.isEmpty()) return ResponseEntity.notFound().build();
        Team opponentTeam = opponentTeamOpt.get();

        Optional<Submission> team1CurrentSubmission = teamService.getCurrentSubmission(selfTeam.getId());
        Optional<Submission> team2CurrentSubmission = teamService.getCurrentSubmission(opponentTeam.getId());

        if (team1CurrentSubmission.isEmpty() || team2CurrentSubmission.isEmpty()) {
            throw new IllegalArgumentException("Both teams must have submission");
        }

        Long remainingAllowedScrimmages = scrimmageMatchService.remainingAllowedScrimmages(selfTeam.getId());
        if (number > remainingAllowedScrimmages) {
            throw new IllegalArgumentException("Your team only has " + remainingAllowedScrimmages + " scrimmages allowed at this time");
        }

        List<ScrimmageMatchDTO> scrimmages = new ArrayList<ScrimmageMatchDTO>();
        for (int i = 0; i < number; i++) {
            GameMatch match = gameMatchService.submitGameMatch(
                selfTeam.getId(),
                opponentTeam.getId(),
                team1CurrentSubmission.get().getId(),
                team2CurrentSubmission.get().getId(),
                MATCH_REASON.SCRIMMAGE,
                map);
            ScrimmageMatchDTO scrimmageMatchDTO = ScrimmageMatchDTO.fromEntity(scrimmageMatchService.createScrimmageMatchData(match, selfTeam));
            scrimmages.add(scrimmageMatchDTO);
        }
        return ResponseEntity.ok(scrimmages);
    }

    @GetMapping("/remaining-scrimmages")
    public ResponseEntity<Long> getRemainingScrimmages() {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Team team = playerService.getTeamFromUUID(UUID.fromString(authId));
        return ResponseEntity.ok(scrimmageMatchService.remainingAllowedScrimmages(team.getId()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleException(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}

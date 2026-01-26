package org.bytefight.webserver.submission.infra;

import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.DefaultLadders;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.permissions.application.PermissionsService;
import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.rabbitmq.application.RabbitMQService;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.submission.application.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.SubmissionDTO;
import org.bytefight.webserver.submission.domain.UploadSubmissionDto;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/submission")
public class SubmissionController {
    private final SubmissionService submissionService;
    private final GameMatchService gameMatchService;
    private final RabbitMQService rabbitMQService;
    private final TeamService teamService;
    private final PlayerService playerService;
    private final PermissionsService permissionsService;

    @PostMapping(path = "/team/{teamUuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionDTO> uploadSubmission(
            @AuthenticationPrincipal User user,
            @PathVariable UUID teamUuid,
            UploadSubmissionDto uploadSubmissionDto
    ) {
//        permissionsService.validateAllowNewSubmission();

        Player player = playerService.getPlayer(user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Team team = teamService.getTeamByUuid(teamUuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if(!teamService.isMember(team, player)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this team");
        }

        Submission submission = null;

        try {
            submission = submissionService.createSubmission(team, uploadSubmissionDto.getDescription(), uploadSubmissionDto.getFile(), uploadSubmissionDto.getIsAutoSet());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }

        GameMatch validation = gameMatchService.createMatch(
                user,
                team,
                team,
                submission,
                submission,
                DefaultLadders.VALIDATION,
                MatchReason.validation,
                null
        );

        gameMatchService.scheduleMatch(validation);

        return ResponseEntity.ok(SubmissionDTO.from(submission));
    }

    @GetMapping("get-download-url")
    public ResponseEntity<DownloadLinkDto> getSubmissionDownloadUrl(@AuthenticationPrincipal User user, @RequestParam String submissionUuid) {
        return ResponseEntity.ok(submissionService.getSubmissionDownloadUri(submissionUuid, user));
    }

//    @GetMapping("/team")
//    public ResponseEntity<List<SubmissionDTO>> getTeamSubmissions(@AuthenticationPrincipal User user) {
//        Player player = playerService.getPlayer(user);
//        Long teamId = player.getTeam().getId();
//        return ResponseEntity.ok(submissionService.getTeamSubmissions(teamId));
//    }
//
//    @DeleteMapping("")
//    public ResponseEntity<SubmissionDTO> deleteSubmission(@AuthenticationPrincipal User user, @RequestParam String submissionUuid) {
//        Player player = playerService.getPlayer(user);
//        Long teamId = player.getTeam().getId();
//
//        Submission deleted = submissionService.deleteSubmission(submissionUuid, teamId);
//
//        return ResponseEntity.ok(SubmissionDTO.from(deleted));
//    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleException(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}

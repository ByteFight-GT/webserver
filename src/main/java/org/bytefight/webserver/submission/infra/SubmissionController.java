package org.bytefight.webserver.submission.infra;

import jakarta.transaction.Transactional;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.common.domain.PermissionDeniedException;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.DefaultLadders;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.submission.application.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.dto.SubmissionDto;
import org.bytefight.webserver.submission.domain.dto.UploadSubmissionDto;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/submission")
@Tag(name = "Submissions", description = "Upload submissions and access submission files")
public class SubmissionController {
    private final SubmissionService submissionService;
    private final GameMatchService gameMatchService;
    private final TeamService teamService;
    private final PlayerService playerService;

    @GetMapping("/team/{teamUuid}")
    @Operation(summary = "List all submissions for a team")
    public ResponseEntity<List<SubmissionDto>> getAllSubmissionsByTeam(
            @AuthenticationPrincipal User user,
            @PathVariable UUID teamUuid
    ) {
        Player player = playerService.getPlayer(user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Team team = teamService.getTeamByUuid(teamUuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if(!teamService.isMember(team, player)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this team");
        }

        return ResponseEntity.ok(submissionService.listSubmissionsByTeam(team).stream().map(SubmissionDto::from).toList());
    }

    @PostMapping(path = "/team/{teamUuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "uploadSubmission",
            summary = "Upload a submission for a team"
    )
    @Transactional
    public ResponseEntity<SubmissionDto> uploadSubmission(
            @AuthenticationPrincipal User user,
            @PathVariable UUID teamUuid,
            UploadSubmissionDto uploadSubmissionDto
    ) {
        Player player = playerService.getPlayer(user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Team team = teamService.getTeamByUuid(teamUuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if(!teamService.isMember(team, player)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this team");
        }

        if(!team.getCompetition().isAllowNewSubmission()) {
            throw new PermissionDeniedException("You are not allowed to create a new submission at this time");
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
                null,
                null
        );

        gameMatchService.scheduleMatch(validation);

        return ResponseEntity.ok(SubmissionDto.from(submission));
    }

    @GetMapping("get-download-url")
    @Operation(
            operationId = "getSubmissionDownloadUrl",
            summary = "Get a signed download URL for a submission"
    )
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
//    public ResponseEntity<SubmissionDto> deleteSubmission(@AuthenticationPrincipal User user, @RequestParam String submissionUuid) {
//        Player player = playerService.getPlayer(user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
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

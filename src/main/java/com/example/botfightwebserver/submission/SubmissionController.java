package com.example.botfightwebserver.submission;

import com.example.botfightwebserver.gameMatch.GameMatch;
import com.example.botfightwebserver.gameMatch.GameMatchJob;
import com.example.botfightwebserver.gameMatch.GameMatchService;
import com.example.botfightwebserver.gameMatch.MATCH_REASON;
import com.example.botfightwebserver.player.Player;
import com.example.botfightwebserver.player.PlayerService;
import com.example.botfightwebserver.rabbitMQ.RabbitMQService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/submission")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final GameMatchService gameMatchService;
    private final RabbitMQService rabbitMQService;
    private final PlayerService playerService;

    @PostMapping(consumes= MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionDTO> uploadSubmission(
        @RequestParam("teamId") Long teamId,
        @RequestParam("file") MultipartFile file) {
        String authId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Player player = playerService.getPlayer(UUID.fromString(authId));
        if (!player.getTeamId().equals(teamId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        SubmissionDTO submissionDTO = SubmissionDTO.fromEntity(submissionService.createSubmission(teamId, file));
        // for submitting a validation match
        GameMatch valMatch = gameMatchService.createMatch(teamId, teamId, submissionDTO.id(), submissionDTO.id(),
            MATCH_REASON.VALIDATION,
            "val_map");
        rabbitMQService.enqueueGameMatchJob(GameMatchJob.fromEntity(valMatch));
        return ResponseEntity.ok(submissionDTO);
        }
}

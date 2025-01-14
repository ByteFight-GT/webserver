package com.example.botfightwebserver.submission;

import com.example.botfightwebserver.gameMatch.GameMatch;
import com.example.botfightwebserver.gameMatch.GameMatchJob;
import com.example.botfightwebserver.gameMatch.GameMatchService;
import com.example.botfightwebserver.gameMatch.MATCH_REASON;
import com.example.botfightwebserver.rabbitMQ.RabbitMQService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1/submission")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final GameMatchService gameMatchService;
    private final RabbitMQService rabbitMQService;

    @PostMapping(consumes= MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionDTO> uploadSubmission(
        @RequestParam("teamId") Long teamId,
        @RequestParam("file") MultipartFile file) {
        SubmissionDTO submissionDTO = SubmissionDTO.fromEntity(submissionService.createSubmission(teamId, file));
        GameMatch valMatch = gameMatchService.createMatch(teamId, teamId, submissionDTO.id(), submissionDTO.id(),
            MATCH_REASON.VALIDATION,
            "val_map");
        rabbitMQService.enqueueGameMatchJob(GameMatchJob.fromEntity(valMatch));
        return ResponseEntity.ok(submissionDTO);
        }
}

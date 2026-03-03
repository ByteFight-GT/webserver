package org.bytefight.webserver.user.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematchfile.application.GameMatchFileService;
import org.bytefight.webserver.gamematchfile.domain.GameMatchFile;
import org.bytefight.webserver.gamematchfile.domain.GameMatchFileVisibility;
import org.bytefight.webserver.gamematchfile.domain.dto.GameMatchFileDto;
import org.bytefight.webserver.gamematchfile.domain.dto.GameMatchFileUploadDto;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.user.application.ResumeService;
import org.bytefight.webserver.user.domain.dto.ResumeDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.UUID;

@Tag(name = "ResumeFile", description = "Endpoints for uploading and retrieving resumes")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resume-file")
public class UserController {
    private final ResumeService resumeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "uploadResume",
            summary = "Upload or replace the authenticated user's resume"
    )
    public ResponseEntity<ResumeDto> uploadResume(
            @AuthenticationPrincipal User user,
            @RequestPart("file") MultipartFile file
    ) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        try {
            FileRecord saved = resumeService.uploadResume(file, user);

            DownloadLinkDto link = resumeService.getDownloadLink(user);
            return ResponseEntity.ok(ResumeDto.from(link, user));
        } catch (IOException e) {
            // invalid type / empty file / storage failure
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping
    @Operation(
            operationId = "getMyResume",
            summary = "Get the authenticated user's resume + a short-lived download link"
    )
    public ResponseEntity<ResumeDto> getMyResume(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        FileRecord resume = user.getResume();
        if (resume == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No resume uploaded");
        }

        DownloadLinkDto link = resumeService.getDownloadLink(user);
        return ResponseEntity.ok(ResumeDto.from(link, user));
    }
}

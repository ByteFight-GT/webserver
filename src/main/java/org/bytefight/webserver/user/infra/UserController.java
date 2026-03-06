package org.bytefight.webserver.user.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.storage.application.LocalStorageService;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.user.application.ResumeService;
import org.bytefight.webserver.user.domain.dto.ResumeDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;

@Tag(name = "ResumeFile", description = "Endpoints for uploading and retrieving resumes")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user/")
public class UserController {
    private final LocalStorageService localStorageService;
    private final ResumeService resumeService;

    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "uploadResume",
            summary = "Upload or replace the authenticated user's resume"
    )
    public ResponseEntity<ResumeDto> uploadResume(
            @AuthenticationPrincipal User user,
            @RequestPart("file") MultipartFile file
    ) {
        try {
            FileRecord saved = resumeService.uploadResume(file, user);

            DownloadLinkDto link = localStorageService.getDownloadLink(user.getUuid().toString(), Duration.ofMinutes(5));
            return ResponseEntity.ok(ResumeDto.from(link, user));
        } catch (IOException e) {
            // invalid type / empty file / storage failure
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume upload failed. Please try again.");
        }
    }

    @GetMapping
    @Operation(
            operationId = "getMyResume",
            summary = "Get the authenticated user's resume + a short-lived download link"
    )
    public ResponseEntity<ResumeDto> getMyResume(@AuthenticationPrincipal User user) {
        FileRecord resume = user.getResume();
        if (resume == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No resume uploaded");
        }

        DownloadLinkDto link = localStorageService.getDownloadLink(user.getUuid().toString(), Duration.ofMinutes(5));
        return ResponseEntity.ok(ResumeDto.from(link, user));
    }
}
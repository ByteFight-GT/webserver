package org.bytefight.webserver.user.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

import org.bytefight.webserver.storage.application.LocalStorageService;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.user.application.UserService;
import org.bytefight.webserver.user.domain.User;
import org.bytefight.webserver.user.domain.dto.ResumeDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "User (Private)", description = "Endpoints for managing user information")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {
  private final LocalStorageService localStorageService;
  private final UserService userService;

  @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      operationId = "uploadResume",
      summary = "Upload or replace the authenticated user's resume (Max 2 MiB limit)")
  public ResponseEntity<Void> uploadResume(
      @AuthenticationPrincipal User user, @RequestPart("file") MultipartFile file) {
    try {
      FileRecord saved = userService.uploadResume(file, user.getUuid());
      return ResponseEntity.ok().build();
    } catch (IOException e) {
      // invalid type / empty file / storage failure
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Resume upload failed. Please try again.");
    }
  }

  @GetMapping(value = "/resume")
  @Operation(
      operationId = "getResume",
      summary = "Get the authenticated user's resume + a short-lived download link")
  @Transactional
  public ResponseEntity<ResumeDto> getResume(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(userService.getResume(user.getUuid()));
  }
}

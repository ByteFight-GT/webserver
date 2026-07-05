package org.bytefight.webserver.player.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.player.domain.AvatarDto;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.player.domain.SelfPlayerDto;
import org.bytefight.webserver.player.domain.UpdatePlayerProfileDto;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.user.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Player (Private)")
@RestController
@RequestMapping("/api/v1/player")
@RequiredArgsConstructor
@Validated
public class PrivatePlayerController {
  private final PlayerService playerService;
  private final TeamService teamService;

  @Operation(operationId = "getCurrentPlayer", summary = "Get current player profile")
  @GetMapping("/me")
  public ResponseEntity<SelfPlayerDto> getCurrentPlayer(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(
        SelfPlayerDto.from(
            playerService
                .getPlayer(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))));
  }

  @Operation(operationId = "updateCurrentPlayer", summary = "Update current player profile")
  @PatchMapping("/me")
  public ResponseEntity<SelfPlayerDto> updateCurrentPlayer(
      @AuthenticationPrincipal User user, @Valid @RequestBody UpdatePlayerProfileDto input) {
    //        permissionsService.validateAllowUpdateProfile();
    Player player =
        playerService
            .getPlayer(user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (input.getUsername() != null) {
      playerService.setUsername(player, input.getUsername());
    }

    return ResponseEntity.ok(SelfPlayerDto.from(player));
  }

  @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      operationId = "uploadAvatar",
      summary = "Upload or replace the authenticated player's avatar (Max 5 MiB limit)")
  public ResponseEntity<Void> uploadAvatar(
      @AuthenticationPrincipal User user, @RequestPart("file") MultipartFile file) {
    try {
      playerService.uploadAvatar(file, user.getUuid());
      return ResponseEntity.ok().build();
    } catch (IOException | IllegalArgumentException e) {
      // invalid type / empty file / storage failure
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Avatar upload failed. Please try again.");
    }
  }

  @DeleteMapping(value = "/avatar")
  @Operation(operationId = "deleteAvatar", summary = "Delete the authenticated player's avatar")
  public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal User user) {
    try {
      playerService.deleteAvatar(user.getUuid());
      return ResponseEntity.noContent().build();
    } catch (NoSuchElementException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping(value = "/avatar")
  @Operation(
      operationId = "getAvatar",
      summary = "Get the authenticated player's avatar + a short-lived download link")
  public ResponseEntity<AvatarDto> getAvatar(@AuthenticationPrincipal User user) {
    try {
      return ResponseEntity.ok(playerService.getAvatar(user.getUuid()));
    } catch (NoSuchElementException e) {
      return ResponseEntity.notFound().build();
    }
  }
}

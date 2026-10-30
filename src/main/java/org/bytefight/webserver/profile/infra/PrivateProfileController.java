package org.bytefight.webserver.profile.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.profile.application.ProfileService;
import org.bytefight.webserver.profile.domain.ProfileDto;
import org.bytefight.webserver.profile.domain.ProfileRequest;
import org.bytefight.webserver.profile.domain.SuggestedProfileDto;
import org.bytefight.webserver.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Profile (Private)")
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class PrivateProfileController {
  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final int MAX_PAGE_SIZE = 100;

  private final PlayerService playerService;
  private final ProfileService profileService;

  @Operation(operationId = "getCurrentProfile", summary = "Get the current player's profile")
  @GetMapping("/me")
  public ResponseEntity<ProfileDto> getCurrentProfile(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(profileService.getProfile(requirePlayer(user)));
  }

  @Operation(operationId = "createCurrentProfile", summary = "Create the current player's profile")
  @PostMapping("/me")
  public ResponseEntity<ProfileDto> createCurrentProfile(
      @AuthenticationPrincipal User user, @Valid @RequestBody ProfileRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(profileService.createProfile(requirePlayer(user), request));
  }

  @Operation(operationId = "updateCurrentProfile", summary = "Replace the current player's profile")
  @PutMapping("/me")
  public ResponseEntity<ProfileDto> updateCurrentProfile(
      @AuthenticationPrincipal User user, @Valid @RequestBody ProfileRequest request) {
    return ResponseEntity.ok(profileService.updateProfile(requirePlayer(user), request));
  }

  @Operation(operationId = "deleteCurrentProfile", summary = "Delete the current player's profile")
  @DeleteMapping("/me")
  public ResponseEntity<Void> deleteCurrentProfile(@AuthenticationPrincipal User user) {
    profileService.deleteProfile(requirePlayer(user));
    return ResponseEntity.noContent().build();
  }

  @Operation(
      operationId = "getSuggestedProfiles",
      summary = "Suggest players with similar profile details")
  @GetMapping("/me/suggestions")
  public ResponseEntity<Page<SuggestedProfileDto>> getSuggestions(
      @AuthenticationPrincipal User user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
    return ResponseEntity.ok(
        profileService.getSuggestions(
            requirePlayer(user), PageRequest.of(normalizePage(page), normalizeSize(size))));
  }

  private Player requirePlayer(User user) {
    return playerService
        .getPlayer(user)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
  }

  private int normalizePage(int page) {
    return Math.max(page, 0);
  }

  private int normalizeSize(int size) {
    return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
  }
}

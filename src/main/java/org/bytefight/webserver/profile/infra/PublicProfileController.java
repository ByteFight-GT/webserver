package org.bytefight.webserver.profile.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.profile.application.ProfileService;
import org.bytefight.webserver.profile.application.ProfileStatsService;
import org.bytefight.webserver.profile.domain.ProfileDto;
import org.bytefight.webserver.profile.domain.dto.PlayerCompetitionDto;
import org.bytefight.webserver.profile.domain.dto.ProfileStatsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Profile (Public)")
@RestController
@RequestMapping("/api/v1/public/profile")
@RequiredArgsConstructor
@Validated
public class PublicProfileController {
  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final int MAX_PAGE_SIZE = 100;

  private final ProfileService profileService;
  private final ProfileStatsService profileStatsService;
  private final PlayerService playerService;

  @GetMapping
  @Operation(operationId = "searchPlayerProfiles", summary = "Search public player profiles")
  public ResponseEntity<Page<ProfileDto>> searchProfiles(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) String major,
      @RequestParam(required = false) String school,
      @RequestParam(required = false) @Min(1900) @Max(2200) Integer graduationYear,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
    return ResponseEntity.ok(
        profileService.searchProfiles(
            query,
            major,
            school,
            graduationYear,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE))));
  }

  @GetMapping("/player/{playerUuid}")
  @Operation(
      operationId = "getPublicPlayerProfile",
      summary = "Get a public profile by player UUID")
  public ResponseEntity<ProfileDto> getProfile(@PathVariable UUID playerUuid) {
    return ResponseEntity.ok(
        profileService
            .findPublicProfile(playerUuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }

  @GetMapping("/{uuid}/competitions")
  @Operation(
      operationId = "getProfileCompetitions",
      summary = "Get PlayerCompetitionDtos associated with a player's profile")
  public ResponseEntity<List<PlayerCompetitionDto>> getProfileCompetitions(
      @PathVariable UUID uuid) {
    Player player =
        playerService
            .getPlayer(uuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    return ResponseEntity.ok(profileService.getCompetitionsByPlayer(player));
  }

  @GetMapping("/{uuid}/stats")
  @Operation(
      operationId = "getProfileStats",
      summary = "Get ProfileStatsDto associated with a player's profile")
  public ResponseEntity<ProfileStatsDto> getProfileStats(@PathVariable UUID uuid) {
    Player player =
        playerService
            .getPlayer(uuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return ResponseEntity.ok(
        profileStatsService
            .getStatsByPlayer(player)
            .map(ProfileStatsDto::from)
            .orElseGet(ProfileStatsDto::notYetComputed));
  }
}

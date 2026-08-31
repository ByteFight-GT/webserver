package org.bytefight.webserver.profile.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.profile.application.ProfileService;
import org.bytefight.webserver.profile.application.ProfileStatsService;
import org.bytefight.webserver.profile.domain.dto.PlayerCompetitionDto;
import org.bytefight.webserver.profile.domain.dto.ProfileStatsDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Profile (Public)")
@RestController
@RequestMapping("/api/v1/public/profile")
@RequiredArgsConstructor
public class PublicProfileController {
  private final ProfileService profileService;
  private final ProfileStatsService profileStatsService;
  private final PlayerService playerService;

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

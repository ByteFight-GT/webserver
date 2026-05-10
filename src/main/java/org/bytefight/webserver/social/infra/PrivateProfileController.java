package org.bytefight.webserver.social.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.social.application.ProfileService;
import org.bytefight.webserver.social.domain.dto.PublicProfileDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Profile (Private)")
@RestController
@RequestMapping("api/v1/private/profiles")
@RequiredArgsConstructor
public class PrivateProfileController {

    private final ProfileService profileService;
    private final PlayerService playerService;

    @PostMapping
    @Operation(
            operationId = "createProfile",
            summary = "REST endpoint to create a profile"
    )
    public ResponseEntity<PublicProfileDto> createProfile(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String description,
            @RequestParam String major,
            @RequestParam Integer year
    ) {
        Player player = playerService.getPlayer(user)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileService.createProfile(player, description, major, year));
    }

    @PatchMapping
    @Operation(
            operationId = "updateProfile",
            summary = "REST endpoint to update a profile"
    )
    public ResponseEntity<PublicProfileDto> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) Integer year
    ) {
        Player player = playerService.getPlayer(user)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
        return ResponseEntity.ok(profileService.updateProfile(player, description, major, year));
    }

    @DeleteMapping
    @Operation(
            operationId = "deleteProfile",
            summary = "REST endpoint to delete a profile"
    )
    public ResponseEntity<Void> deleteProfile(
            @AuthenticationPrincipal User user
    ) {
        Player player = playerService.getPlayer(user)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
        profileService.deleteProfile(player);
        return ResponseEntity.noContent().build();
    }
}
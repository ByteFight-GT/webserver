package org.bytefight.webserver.player.infra;

import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.player.domain.PublicPlayerDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Player (Public)")
@RestController
@RequestMapping("/api/v1/public/player")
@RequiredArgsConstructor
@Validated
public class PublicPlayerController {
    private final PlayerService playerService;

    @Operation(
            operationId = "getPublicPlayerByUuid",
            summary = "Get a public player profile"
    )
    @GetMapping("/{uuid}")
    public ResponseEntity<PublicPlayerDto> getPlayerById(@PathVariable UUID uuid) {
        Player player = playerService.getPlayer(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(PublicPlayerDto.from(player));
    }

    @Operation(
            operationId = "checkPublicUsernameAvailability",
            summary = "Check if a username is available"
    )
    @GetMapping("/check-username/{username}")
    public ResponseEntity<Map<String, Boolean>> checkUsernameAvailability(@PathVariable String username) {
        boolean isAvailable = !playerService.isUsernameExist(username);
        return ResponseEntity.ok(Collections.singletonMap("available", isAvailable));
    }
}
